package sslengine;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import handlers.BufferFlusherCompletionHandler;

import javax.net.ssl.SSLException;

public class SLLAsynchronousSocketChannel extends AsynchronousSocketChannel {
	private AsynchronousSocketChannel channel;
	private SSLEngine engine;
	private ByteBuffer myAppData;
	private ByteBuffer myNetData;
	private ByteBuffer peerAppData;
	private ByteBuffer peerNetData;

	public SLLAsynchronousSocketChannel(AsynchronousSocketChannel channel, SSLEngine engine) {
		super(channel.provider());
		this.channel = channel;
		this.engine = engine;
		int appBufferSize = engine.getSession().getApplicationBufferSize();
		int netBufferSize = engine.getSession().getPacketBufferSize();
		myAppData = ByteBuffer.allocate(appBufferSize);
		myNetData = ByteBuffer.allocate(netBufferSize);
		peerAppData = ByteBuffer.allocate(appBufferSize);
		peerNetData = ByteBuffer.allocate(netBufferSize);
	}

	@Override
	public void close() throws IOException {
		if(engine.isOutboundDone()) {
			engine.closeOutbound();
			doHandshake(null, new CompletionHandler<Void, Void>() {
				@Override
				public void completed(Void arg0, Void arg1) {
					try {
						channel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void failed(Throwable e, Void arg1) {
					try {
						channel.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
		}
	}

	@Override
	public boolean isOpen() {
		return channel.isOpen();
	}

	@Override
	public <T> T getOption(SocketOption<T> name) throws IOException {
		return channel.getOption(name);
	}

	@Override
	public Set<SocketOption<?>> supportedOptions() {
		return channel.supportedOptions();
	}

	@Override
	public AsynchronousSocketChannel bind(SocketAddress local) throws IOException {
		return channel.bind(local);
	}

	@Override
	public <T> AsynchronousSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
		return channel.setOption(name, value);
	}

	@Override
	public AsynchronousSocketChannel shutdownInput() throws IOException {
		return channel.shutdownInput();
	}

	@Override
	public AsynchronousSocketChannel shutdownOutput() throws IOException {
		return channel.shutdownOutput();
	}

	@Override
	public SocketAddress getRemoteAddress() throws IOException {
		return channel.getRemoteAddress();
	}

	@Override
	public <A> void connect(SocketAddress remote, A attachment, CompletionHandler<Void, ? super A> handler) {
		channel.connect(remote, attachment, new CompletionHandler<Void, A>() {
			@Override
			public void completed(Void result, A attachment) {
				try {
					engine.beginHandshake();
					doHandshake(attachment, handler);
				} catch (SSLException e) {
					handler.failed(e, attachment);
				}
			}

			@Override
			public void failed(Throwable exc, A attachment) {
				handler.failed(exc, attachment);
			}
		});
	}

	@Override
	public Future<Void> connect(SocketAddress remote) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A> void read(ByteBuffer dst, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
		if(dispatchData(dst, attachment, handler)) {
			return;
		}

		boolean needsRead = attemptDecrypt(dst, attachment, handler);

		if (needsRead) {
			channel.read(peerNetData, timeout, unit, attachment, new CompletionHandler<Integer, A>() {
				@Override
				public void completed(Integer result, A attachment) {
					if (result >= 0) {
						boolean needsRead = attemptDecrypt(dst, attachment, handler);
						if (needsRead) {
							channel.read(peerNetData, timeout, unit, attachment, this);
						}
					} else {
						try {
							close();
							handler.completed(result, attachment);
						} catch (IOException e) {
							handler.failed(e, attachment);
						}
					}
				}

				@Override
				public void failed(Throwable exc, A attachment) {
					handler.failed(exc, attachment);
				}
			});
		}
	}

	private <A> boolean dispatchData(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler) {
		peerAppData.flip();

		if(peerAppData.hasRemaining()) {
			int transferred = Math.min(dst.remaining(), peerAppData.remaining());

			if (peerAppData.remaining() > dst.remaining()) {
				// the ByteBuffer bulk copy only works if the src has <= remaining of the dst. narrow the view of src here to make use of i
				int newLimit = peerAppData.position() + transferred;
				ByteBuffer src = peerAppData.duplicate();
				src.limit(newLimit);
				dst.put(src);
				peerAppData.position(peerAppData.position() + transferred);
			} else {
				dst.put(peerAppData);
			}

			peerAppData.compact();
			handler.completed(transferred, attachment);
			return true;
		}
		
		peerAppData.clear();
		return false;
	}

	@Override
	public Future<Integer> read(ByteBuffer dst) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A> void read(ByteBuffer[] dsts, int offset, int length, long timeout, TimeUnit unit, A attachment,
			CompletionHandler<Long, ? super A> handler) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <A> void write(ByteBuffer src, long timeout, TimeUnit unit, A attachment, CompletionHandler<Integer, ? super A> handler) {
		boolean doneWrap = true;
		do {
			try {
				myNetData.compact();
				SSLEngineResult result = engine.wrap(src, myNetData);
				switch (result.getStatus()) {
					case OK:
						this.flipAndFlushBuffer(myNetData, channel,
							new CompletionHandler<Void, Void>() {
								@Override
								public void completed(Void rs, Void att) {
									handler.completed(result.bytesConsumed(), attachment);
								}

								@Override
								public void failed(Throwable exc, Void att) {
									handler.failed(exc, attachment);
								}
							});
						break;
					case BUFFER_OVERFLOW:
						myNetData = enlargePacketBuffer(engine, myNetData);
						doneWrap = false;
						break;
					case BUFFER_UNDERFLOW:
						handler.failed(new Exception("Buffer underflow when encrypting TLS message"), attachment);
						break;
					default:
						break;
				}
			} catch (SSLException e) {
				handler.failed(e, attachment);
				return;
			}
		} while (!doneWrap);
	}

	@Override
	public Future<Integer> write(ByteBuffer src) {
		throw new UnsupportedOperationException();

	}

	@Override
	public <A> void write(ByteBuffer[] srcs, int offset, int length, long timeout, TimeUnit unit, A attachment,
			CompletionHandler<Long, ? super A> handler) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SocketAddress getLocalAddress() throws IOException {
		return channel.getLocalAddress();
	}

	public <A> void doHandshake(A attachment, CompletionHandler<Void, A> handler) {
		HandshakeStatus handshakeStatus = engine.getHandshakeStatus();

		if (handshakeStatus == SSLEngineResult.HandshakeStatus.FINISHED
				|| handshakeStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
			handler.completed(null, attachment);
		}
		switch (handshakeStatus) {
			case NEED_UNWRAP:
				if(peerNetData.remaining() < peerNetData.capacity()) {
					unwrapRemaining(attachment, handler);
				} else {
					channel.read(peerNetData, 2, TimeUnit.SECONDS, attachment, new CompletionHandler<Integer, A>() {
						@Override
						public void completed(Integer count, A attachment) {
							if (count < 0) {
								if (engine.isInboundDone() && engine.isOutboundDone()) {
									handler.failed(new Exception("SSL connection incomplete"), attachment);
								} else {
									try {
										engine.closeInbound();
									} catch (SSLException e) {
										// ignore
									}
									engine.closeOutbound();
								}
							} else {
								unwrapRemaining(attachment, handler);
							}
						}

						@Override
						public void failed(Throwable exc, A attachment) {
							handler.failed(exc, attachment);
						}
					});
				}
				break;
			case NEED_WRAP:
				myNetData.clear();
				try {
					SSLEngineResult result = engine.wrap(myAppData, myNetData);
					switch (result.getStatus()) {
						case OK:
							this.flipAndFlushBuffer(myNetData, channel,
								new CompletionHandler<Void, Void>() {
									@Override
									public void completed(Void result, Void localAttachment) {
										doHandshake(attachment, handler);
									}

									@Override
									public void failed(Throwable exc, Void localAttachment) {
										handler.failed(exc, attachment);
									}
								});
							break;
						case BUFFER_OVERFLOW:
							myNetData = enlargePacketBuffer(engine, myNetData);
							doHandshake(attachment, handler);
							break;
						case BUFFER_UNDERFLOW:
							handler.failed(new SSLException(
									"Buffer underflow occured after a wrap. I don't think we should ever get here."),
									attachment);
						case CLOSED:
							this.flipAndFlushBuffer(myNetData, channel,	
								new CompletionHandler<Void, Void>() {

									@Override
									public void completed(Void result, Void localAttachment) {
										peerNetData.clear();
										doHandshake(attachment, handler);
									}

									@Override
									public void failed(Throwable exc, Void localAttachment) {
									}
								});
							break;
						default:
							handler.failed(new Exception("Invalid SSL status: " + result.getStatus()), attachment);
					}
				} catch (SSLException sslException) {
					engine.closeOutbound();
					handshakeStatus = engine.getHandshakeStatus();
					break;
				}
				break;
			case NEED_TASK:
				Runnable task;
				while ((task = engine.getDelegatedTask()) != null) {
					task.run();
				}
				doHandshake(attachment, handler);
				break;
			case FINISHED:
				break;
			case NOT_HANDSHAKING:
				break;
			default:
				handler.failed(new Exception("Invalid SSL status: " + handshakeStatus), attachment);
		}
	}

	private ByteBuffer enlargePacketBuffer(SSLEngine engine, ByteBuffer buffer) {
		return enlargeBuffer(buffer, engine.getSession().getPacketBufferSize());
	}

	private ByteBuffer enlargeApplicationBuffer(SSLEngine engine, ByteBuffer buffer) {
		return enlargeBuffer(buffer, engine.getSession().getApplicationBufferSize());
	}

	private ByteBuffer enlargeBuffer(ByteBuffer buffer, int sessionProposedCapacity) {
		if (sessionProposedCapacity > buffer.capacity()) {
			buffer = ByteBuffer.allocate(sessionProposedCapacity);
		} else {
			buffer = ByteBuffer.allocate(buffer.capacity() * 2);
		}
		return buffer;
	}

	private ByteBuffer handleBufferUnderflow(SSLEngine engine, ByteBuffer buffer) {
		if (engine.getSession().getPacketBufferSize() < buffer.limit()) {
			return buffer;
		} else {
			ByteBuffer replaceBuffer = enlargePacketBuffer(engine, buffer);
			buffer.flip();
			replaceBuffer.put(buffer);
			return replaceBuffer;
		}
	}

	private <A> boolean attemptDecrypt(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler) {
		SSLEngineResult result;
		try {
			peerNetData.flip();
			result = engine.unwrap(peerNetData, peerAppData);
			peerNetData.compact();
			switch (result.getStatus()) {
				case OK:
					dispatchData(dst, attachment, handler);
					return false;
				case BUFFER_OVERFLOW:
					handler.failed(new Exception("Buffer overflow when decrypting TLS message"), attachment);
					return false;
				case BUFFER_UNDERFLOW:
					return true;
				default:
					break;
			}
		} catch (SSLException e) {
			handler.failed(e, attachment);
		}

		return false;
	}

	private <A> void unwrapRemaining(A attachment, CompletionHandler<Void, A> handler) {
		peerNetData.flip();
		try {
			SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
			peerNetData.compact();
			switch (result.getStatus()) {
				case OK:
					break;
				case BUFFER_OVERFLOW:
					SLLAsynchronousSocketChannel.this.peerAppData = enlargeApplicationBuffer(engine, peerAppData);
					break;
				case BUFFER_UNDERFLOW:
					peerNetData = handleBufferUnderflow(engine, peerNetData);
					break;
				case CLOSED:
					if (engine.isOutboundDone()) {
						handler.failed(new Exception("SSL connection incomplete"), attachment);
					} else {
						engine.closeOutbound();
					}
					break;
				default:
					handler.failed(new Exception("Invalid SSL status: " + result.getStatus()),
							attachment);
			}
			doHandshake(attachment, handler);
		} catch (SSLException sslException) {
			sslException.printStackTrace();
			engine.closeOutbound();
		}
	}

	public void flushBuffer(ByteBuffer buffer, AsynchronousSocketChannel channelToSend, CompletionHandler<Void, Void> completionHandler) {
	    channelToSend.write(buffer, 2, TimeUnit.SECONDS, buffer, new BufferFlusherCompletionHandler(channelToSend, completionHandler));
	}

	private void flipAndFlushBuffer(ByteBuffer buffer, AsynchronousSocketChannel channelToSend, CompletionHandler<Void, Void> completionHandler) {
	    buffer.flip();
	    flushBuffer(buffer, channelToSend, completionHandler);
	}
}