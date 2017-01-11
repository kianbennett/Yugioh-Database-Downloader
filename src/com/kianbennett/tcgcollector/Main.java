package com.kianbennett.tcgcollector;

import javafx.scene.input.DataFormat;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {

    public static void main(String[] args) {
        boolean min = false;
        String out = "cards.json";
        String error = null;
        for(int a = 0; a < args.length; a++) {
            String arg = args[a];
            if(arg.equals("-min")) {
                min = true;
                continue;
            }
            if(arg.equals("-o")) {
                if(a + 1 < args.length && !args[a + 1].startsWith("-")) {
                    out = args[a + 1];
                    if(!out.contains(".")) {
                        error = "Output file name needs an extension";
                    }
                } else {
                    error = "Use the output command -o <filename>";
                }
            }
        }

        if(error == null) {
            CardDatabase database = new CardDatabase(new Date(), min, out);
        } else {
            System.err.println(error);
        }
    }
}