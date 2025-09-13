package com.p2plink;

import com.p2plink.controller.FileController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args)  {
       try {
           FileController fileController = new FileController(8080,9090);
           fileController.start();
           System.out.println("P2P Link Server Started at http://localhost:8080/");
           System.out.println("UI Server Started at http://localhost:3000/");


           Runtime.getRuntime().addShutdownHook(new Thread(()->{
               System.out.println("Shutting down P2P Link Server");
               fileController.stop();
           }));

           System.out.println("Press Enter to Stop Server");
           BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
           String input = reader.readLine(); // waits until user presses ENTER
           if (input == null || input.trim().isEmpty() || "exit".equalsIgnoreCase(input.trim())) {
               System.out.println("Stopping server...");
               fileController.stop();
           }
       }
       catch (IOException e) {
           System.err.println("Failed to start server at port 8080");
           e.printStackTrace();

       }


    }
}