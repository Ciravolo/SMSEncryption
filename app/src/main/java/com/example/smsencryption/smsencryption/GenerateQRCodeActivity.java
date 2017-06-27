package com.example.smsencryption.smsencryption;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.spec.SecretKeySpec;

public class GenerateQRCodeActivity extends AppCompatActivity {

    ImageView imageViewQRCode;
    TextView txtTest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qrcode);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        imageViewQRCode = (ImageView) findViewById(R.id.imageViewQRCode);

        try {
            String randomSeed = generateRandomizedString1024Bits();
            setQRCode(randomSeed);
            Constants.setW(randomSeed);
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }


    public String generateRandomizedString1024Bits() throws NoSuchAlgorithmException, UnsupportedEncodingException{
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[128];
        random.nextBytes(bytes);
        String randomGeneratedString = new String(Base64.encodeBase64(bytes));
        randomGeneratedString.replace('+','-').replace('/','_');
        return randomGeneratedString;
    }


    public String generateRandomSeed(){
        Random r = new Random(System.currentTimeMillis());
        int number = 10000 + r.nextInt(20000);
        return String.valueOf(number);
    }

    public void setQRCode(String qrText){

        QRCodeWriter writer = new QRCodeWriter();

        try {

            BitMatrix bitMatrix = writer.encode(qrText, BarcodeFormat.QR_CODE, 1024, 1024);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            imageViewQRCode.setImageBitmap(bmp);

        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private static byte[] decodeBase64(String dataToDecode)
    {
        byte[] dataDecoded = Base64.decodeBase64(dataToDecode);
        return dataDecoded;
    }

    //encode data in base 64
    private static byte[] encodeBase64(byte[] dataToEncode)
    {
        byte[] dataEncoded = Base64.encodeBase64(dataToEncode);
        return dataEncoded;
    }

}
