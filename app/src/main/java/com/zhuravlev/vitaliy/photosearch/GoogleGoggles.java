package com.zhuravlev.vitaliy.photosearch;

import android.content.Context;
import android.util.Log;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GoogleGoggles {
    private final Context context;
    // The POST body required to validate the CSSID.
    private byte[] cssidPostBody = new byte[]{0x22, 0x00, 0x62, 0x3C, 0x0A, 0x13, 0x22, 0x02, 0x65, 0x6E, (byte) 0xBA, (byte) 0xD3, (byte) 0xF0, 0x3B, 0x0A, 0x08, 0x01, 0x10, 0x01, 0x28,
            0x01, 0x30, 0x00, 0x38, 0x01, 0x12, 0x1D, 0x0A, 0x09, 0x69, 0x50, 0x68, 0x6F, 0x6E, 0x65, 0x20, 0x4F, 0x53, 0x12, 0x03,
            0x34, 0x2E, 0x31, 0x1A, 0x00, 0x22, 0x09, 0x69, 0x50, 0x68, 0x6F, 0x6E, 0x65, 0x33, 0x47, 0x53, 0x1A, 0x02, 0x08, 0x02,
            0x22, 0x02, 0x08, 0x01};

    // Bytes trailing the image byte array. Look at the next code snippet to see
    // where it is used in sendPhoto() method.
    private byte[] trailingBytes = new byte[]{24, 75, 32, 1, 48, 0, (byte) 146,
            (byte) 236, (byte) 244, 59, 9, 24, 0, 56, (byte) 198, (byte) 151, (byte) 220, (byte) 223, (byte) 247, 37, 34, 0};

    // Generates a cssid.
    private String cssid;

    public GoogleGoggles(Context context) {
        this.context = context;
    }

    // Encodes an int32 into varint32.
    public static byte[] toVarint32(int value) {
        List<Byte> bytes = new ArrayList<>();

        while ((0x7F & value) != 0) {
            int i = (0x7F & value);
            if ((0x7F & (value >> 7)) != 0)
                i += 128;

            bytes.add((byte) i);
            value = value >> 7;
        }

        byte[] array = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++)
            array[i] = bytes.get(i);
        return array;
    }

    // Validates the CSSID we just created, by POSTing it to Goggles.
    public void validateCSSID(String cssid) throws Exception {
        String urlString = String.format("http://www.google.com/goggles/container_proto?cssid=%s", cssid);
        Log.d("URL path", String.format("http://www.google.com/goggles/container_proto?cssid=%s", cssid));
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");

        connection.setDoOutput(true);

        addHeaders(connection);

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(cssidPostBody);
        outputStream.close();

        connection.setReadTimeout(3000);
        connection.connect();

        String message = connection.getResponseCode() + " " + connection.getResponseMessage();
//        ((MainActivity) context).setResponseText(message);
        Log.d("Response", message);
    }

    private void addHeaders(HttpURLConnection connection) {
        connection.addRequestProperty("Content-Type", "application/x-protobuffer");
        connection.addRequestProperty("Pragma", "no-cache");
        connection.addRequestProperty("Connection", "Keep-Alive");
    }

    public String getCssid() {
        Random random = new Random(System.currentTimeMillis());
        int max = 0x7FFFFFFF;
        int min = 0x10000000;
        return String.format("%s%s", Integer.toHexString(random.nextInt(max - min) + min), Integer.toHexString(random.nextInt(max - min) + min));
    }

    // Conducts an image search by POSTing an image to Goggles, along with a valid CSSID.
    public void sendPhoto(String cssid, byte[] image) throws Exception {

        String urlString = String.format("http://www.google.com/goggles/container_proto?cssid=%s", cssid);
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();

        addHeaders(connection);
        connection.setRequestMethod("POST");

        // x = image size
        int x = image.length;
        byte[] xVarint = GoogleGoggles.toVarint32(x);

        // a = x + 32
        byte[] aVarint = GoogleGoggles.toVarint32(x + 32);

        // b = x + 14
        byte[] bVarint = GoogleGoggles.toVarint32(x + 14);

        // c = x + 10
        byte[] cVarint = GoogleGoggles.toVarint32(x + 10);

        OutputStream stream = connection.getOutputStream();
        stream.write(cssidPostBody);

        // 0x0A
        stream.write(new byte[]{10}, 0, 1);

        // a
        stream.write(aVarint, 0, aVarint.length);

        // 0x0A
        stream.write(new byte[]{10}, 0, 1);

        // b
        stream.write(bVarint, 0, bVarint.length);

        // 0x0A
        stream.write(new byte[]{10}, 0, 1);

        // c
        stream.write(cVarint, 0, cVarint.length);

        // 0x0A
        stream.write(new byte[]{10}, 0, 1);

        // x
        stream.write(xVarint, 0, xVarint.length);

        // Write image
        stream.write(image, 0, image.length);

        // Write trailing bytes
        stream.write(
                trailingBytes,
                0,
                trailingBytes.length);

        stream.close();

        connection.connect();
        String message = connection.getResponseCode() + " " + connection.getResponseMessage();
                ((MainActivity) context).setResponseText(message);
        Log.d("Response", message);
    }
}