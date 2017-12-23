package fakedex.com.cx.fake;


import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


public class AES {

    public static final String DEFAULT_PWD = "abcdefghijklmnop";

    private static final String algorithmStr = "AES/ECB/PKCS5Padding";

    private static Cipher encryptCipher;
    private static Cipher decryptCipher;

    public static void init(String password) {
        try {
            // 生成一个实现指定转换的 Cipher 对象。
            encryptCipher = Cipher.getInstance(algorithmStr);
            decryptCipher = Cipher.getInstance(algorithmStr);// algorithmStr
            byte[] keyStr = password.getBytes();
            SecretKeySpec key = new SecretKeySpec(keyStr, "AES");
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public static byte[] encrypt(byte[] content) {
        try {
            byte[] result = encryptCipher.doFinal(content);
            return result;
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] decrypt(byte[] content) {
        try {
            byte[] result = decryptCipher.doFinal(content);
            return result;
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static void main(String[] args) throws Exception {
//        byte[] newfs = Utils.int2Bytes(2312312);
//        byte[] refs = new byte[4];
//        //高位在前，低位在前掉个个
//        for (int i = 0; i < 4; i++) {
//            refs[i] = newfs[newfs.length - 1 - i];
//        }
//        System.out.println(Arrays.toString(newfs));
//        System.out.println(Arrays.toString(refs));
//
//        ByteBuf byteBuf = Unpooled.buffer();
//
//        byteBuf.writeInt(2312312);
//        byte[] a = new byte[4];
//        byteBuf.order(ByteOrder.LITTLE_ENDIAN);
//        byteBuf.readBytes(a);
//        System.out.println(Arrays.toString(a));

//        AES.init(AES.DEFAULT_PWD);
//        String msg = Base64.encode(AES.encrypt(new byte[]{1, 2, 3, 4, 5}));
//        System.out.println(msg);
//        byte[] aes = AES.decrypt(Base64.decode(msg));
//        System.out.println(Arrays.toString(aes));

        File zip = new File("/Users/xiang/develop/app-debug.apk");
        String absolutePath = zip.getAbsolutePath();
        File dir = new File(absolutePath.substring(0, absolutePath.lastIndexOf(".")));
        Zip.unZip(zip,dir);

        File zip2 = new File("/Users/xiang/develop/app-debug2.apk");
        Zip.zip(dir,zip2);

        String[] argv = {
                "jarsigner","-verbose", "-sigalg", "MD5withRSA",
                "-digestalg", "SHA1",
                "-keystore", "/Users/xiang/develop/debug.keystore",
                "-storepass","android",
                "-keypass", "android",
                "-signedjar", "/Users/xiang/develop/app-debug2-sign.apk",
                "/Users/xiang/develop/app-debug2.apk",
                "androiddebugkey"
        };
        Process pro = null;
        try {
            pro = Runtime.getRuntime().exec(argv);
            //destroy the stream
            try {
                pro.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            if (pro != null) {
                pro.destroy();
            }
        }
    }


}
