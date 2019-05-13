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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private String userName;
    private final static String SERVER_STORAGE = "server_storage/";
    private static String USER_ROOT;
    private static String CURRENT_DIRECTORY;
    private static Stack<String> serverPathStack = new Stack<>();
    private static Map<String, Boolean> fileMap;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null) {
                return;
            }

            if (msg instanceof Auth){
                this.userName = ((Auth) msg).getLogin();
                USER_ROOT = SERVER_STORAGE + userName + "/";
                CURRENT_DIRECTORY = USER_ROOT;
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
                if (!Files.exists(Paths.get(CURRENT_DIRECTORY + fr.getFilename()))){
                    System.out.println("File not exists!");
                    return;
                }

                send(fr.getFilename() + "/", ctx);
            }

            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                if (fm.isDirectory()) {
                    Files.createDirectories(Paths.get(CURRENT_DIRECTORY + fm.getFilename()));

                } else {
                    Files.write(Paths.get(CURRENT_DIRECTORY + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);

                }

            }

        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void send(String filename, ChannelHandlerContext ctx) {
        if (Files.isDirectory(Paths.get(CURRENT_DIRECTORY + filename))){
            FileMessage fm = new FileMessage(filename, CURRENT_DIRECTORY, true);
            ctx.writeAndFlush(fm);
            File dir = new File(CURRENT_DIRECTORY + filename);
            File[] files = dir.listFiles();
            if (files == null || files.length == 0) {
                return;
            }

            for (File f: files
            ) {
                send(filename + f.getName() + "/", ctx);
            }
        } else {
            ctx.writeAndFlush(new FileMessage(filename, CURRENT_DIRECTORY, false));
            return;
        }
    }


    private void executeCommand(CommandMessage msg, ChannelHandlerContext ctx) {
        if (msg instanceof Refresh) {

            ctx.writeAndFlush(new FileMap(formFileMap(CURRENT_DIRECTORY)));
            return;
        }

        if (msg instanceof Delete) {
            Delete delete = (Delete) msg;
            try {
                FileController.delete(CURRENT_DIRECTORY + delete.getFileName() + "/");
                ctx.writeAndFlush(new FileMap(formFileMap(CURRENT_DIRECTORY)));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        if (msg instanceof VistCommand){
            VistCommand vistCommand = (VistCommand) msg;
            serverPathStack.push(CURRENT_DIRECTORY);
            CURRENT_DIRECTORY =  USER_ROOT + vistCommand.getDirectory();
            ctx.writeAndFlush(new FileMap(formFileMap(CURRENT_DIRECTORY )));
        }

        if (msg instanceof Up){
            if (!serverPathStack.empty()){
                CURRENT_DIRECTORY = serverPathStack.pop();
            }

        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private static Map<String, Boolean> formFileMap(String currentDirectory){
        fileMap = new HashMap<>();
        try {
            List<String> list = Files.list(Paths.get(currentDirectory)).map(p -> p.getFileName().toString()).collect(Collectors.toList());
            for (String s: list
                 ) {
                fileMap.put(s, Files.isDirectory(Paths.get(CURRENT_DIRECTORY + s)));
            }
            return fileMap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
