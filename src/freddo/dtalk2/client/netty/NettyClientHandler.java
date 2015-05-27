/*
 * Copyright (c) 2013-2015 ArkaSoft LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package freddo.dtalk2.client.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freddo.dtalk2.DTalkConnection;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;

public class NettyClientHandler extends SimpleChannelInboundHandler<Object> {
  private static final Logger LOG = LoggerFactory.getLogger(NettyClientHandler.class);

  @SuppressWarnings("unused")
  private final DTalkConnection mConnection;
  private final WebSocketClientHandshaker mHandshaker;
  private ChannelPromise handshakeFuture;

  public NettyClientHandler(DTalkConnection conn, WebSocketClientHandshaker handshaker) {
    mConnection = conn;
    mHandshaker = handshaker;
  }

  public ChannelFuture handshakeFuture() {
    return handshakeFuture;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    handshakeFuture = ctx.newPromise();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    mHandshaker.handshake(ctx.channel());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    LOG.debug("WebSocket Client disconnected!");
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
    Channel ch = ctx.channel();
    if (!mHandshaker.isHandshakeComplete()) {
      mHandshaker.finishHandshake(ch, (FullHttpResponse) msg);
      LOG.debug("WebSocket Client connected!");
      handshakeFuture.setSuccess();
      return;
    }

    if (msg instanceof FullHttpResponse) {
      FullHttpResponse response = (FullHttpResponse) msg;
      throw new Exception("Unexpected FullHttpResponse (getStatus=" + response.getStatus() + ", content="
          + response.content().toString(CharsetUtil.UTF_8) + ')');
    }

    WebSocketFrame frame = (WebSocketFrame) msg;
    if (frame instanceof TextWebSocketFrame) {
      TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
      System.out.println("WebSocket Client received message: " + textFrame.text());

      String message = textFrame.text();

      try {



      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    } else if (frame instanceof PongWebSocketFrame) {
      //LOG.d(TAG, "WebSocket Client received pong");
    } else if (frame instanceof CloseWebSocketFrame) {
      //LOG.d(TAG, "WebSocket Client received closing");
      ch.close();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    if (!handshakeFuture.isDone()) {
      handshakeFuture.setFailure(cause);
    }
    ctx.close();
  }
}
