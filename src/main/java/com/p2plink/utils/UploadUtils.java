package com.p2plink.utils;

import java.util.Random;

public class UploadUtils {

    public static int generateCode(){
        int DYNAMIC_STARTING_PORT = 8081;
        int DYNAMIC_ENDING_PORT = 19266;

        Random random = new Random();
        return random.nextInt(DYNAMIC_ENDING_PORT-DYNAMIC_STARTING_PORT);

    }
}
