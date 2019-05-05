package ru.geekbrains.belikov.cloud.server;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import ru.geekbrains.belikov.cloud.common.*;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private String userName;
    private final static String SERVER_STORAGE = "server_storage/";
    private static String USER_DIRECTORY;

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) {
                return;
            }

            if (msg instanceof Auth){
                this.userName = ((Auth) msg).getLogin();
                USER_DIRECTORY = SERVER_STORAGE + userName + "/";
                if (!Files.exists(Paths.get(USER_DIRECTORY))){
                    Files.createDirectories(Paths.get(USER_DIRECTORY));
                }
            }

            if (msg instanceof CommandMessage){
                CommandMessage cm = (CommandMessage) msg;
                executeCommand(cm, ctx);
            }
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get(USER_DIRECTORY + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get(USER_DIRECTORY + fr.getFilename()));
                    ctx.writeAndFlush(fm);
                }
            }

            if (msg instanceof FileMessage){
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get(USER_DIRECTORY + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);

            }

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void executeCommand(CommandMessage msg, ChannelHandlerContext ctx) {
        System.out.println("Execute");
        if (msg instanceof Refresh) {
            ctx.writeAndFlush(new FileList(formFileList()));
            return;
        }

        if (msg instanceof Delete) {
            Delete delete = (Delete) msg;
            try {
                Files.delete(Paths.get(USER_DIRECTORY + delete.getFileName()));
                System.out.println(delete.getFileName());
                ctx.writeAndFlush(new FileList(formFileList()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private static List<String> formFileList(){
        try {
            return Files.list(Paths.get(USER_DIRECTORY)).map(p -> p.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
