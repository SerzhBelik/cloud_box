package ru.geekbrains.belikov.cloud.server;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import ru.geekbrains.belikov.cloud.common.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private String userName;
    private final static String SERVER_STORAGE = "server_storage/";
    private static String CURRENT_DIRECTORY;
    private static Stack<String> serverPathStack = new Stack<>();

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
                CURRENT_DIRECTORY = SERVER_STORAGE + userName + "/";
                if (!Files.exists(Paths.get(CURRENT_DIRECTORY))){
                    Files.createDirectories(Paths.get(CURRENT_DIRECTORY));
                }
            }

            if (msg instanceof CommandMessage){
                CommandMessage cm = (CommandMessage) msg;
                executeCommand(cm, ctx);
            }
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (Files.exists(Paths.get(CURRENT_DIRECTORY + fr.getFilename()))) {
                    FileMessage fm = new FileMessage(Paths.get(CURRENT_DIRECTORY + fr.getFilename()));
                    ctx.writeAndFlush(fm);
                }
            }

            if (msg instanceof FileMessage){
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get(CURRENT_DIRECTORY + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);

            }

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void executeCommand(CommandMessage msg, ChannelHandlerContext ctx) {
        if (msg instanceof Refresh) {
            System.out.println("Refresh " + CURRENT_DIRECTORY);
            ctx.writeAndFlush(new FileList(formFileList(CURRENT_DIRECTORY)));
            return;
        }

        if (msg instanceof Delete) {
            Delete delete = (Delete) msg;
            try {
                Files.delete(Paths.get(CURRENT_DIRECTORY + delete.getFileName()));
                System.out.println(delete.getFileName());
                ctx.writeAndFlush(new FileList(formFileList(CURRENT_DIRECTORY)));
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if (msg instanceof VistCommand){
//            System.out.println("visit");
            VistCommand vistComand = (VistCommand) msg;
//            System.out.println(vistComand.getDirectory());
            serverPathStack.push(CURRENT_DIRECTORY);
            CURRENT_DIRECTORY =  CURRENT_DIRECTORY + vistComand.getDirectory();
//            System.out.println("CUR DIR  " + CURRENT_DIRECTORY);
//            System.out.println(formFileList(CURRENT_DIRECTORY));
            ctx.writeAndFlush(new FileList(formFileList(CURRENT_DIRECTORY )));
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private static List<String> formFileList(String currentDirectory){
        try {
            return Files.list(Paths.get(currentDirectory)).map(p -> p.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
