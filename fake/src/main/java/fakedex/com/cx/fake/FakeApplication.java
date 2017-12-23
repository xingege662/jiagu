package fakedex.com.cx.fake;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by xinchang on 2017/12/9.
 */

public class FakeApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        String fake_key = null;
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo
                    (getPackageName(), PackageManager.GET_META_DATA);
            fake_key = applicationInfo.metaData.getString("fake_key");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(fake_key)) {
            AES.init(AES.DEFAULT_PWD);
        } else {
            AES.init(fake_key);
        }
        File apkFile = new File(getApplicationInfo().sourceDir);
        //data/data/包名/files/fake_apk/
        File unZipFile = getDir("fake_apk", MODE_PRIVATE);
        File app = new File(unZipFile,"app");
        if (!app.exists()) {
            Zip.unZip(apkFile, app);
            File[] files = app.listFiles();
            for (File file : files) {
                String name = file.getName();
                if (name.equals("classes.dex")) {
                    try {
                        Log.d("FAKE","解密");
                        byte[] bytes = getBytes(file);
                        byte[] len = new byte[4];
                        System.arraycopy(bytes, bytes.length - 4, len, 0, 4);
                        int mainlen = Utils.bytes2Int(len);
                        byte[] maindex = new byte[mainlen];
                        System.arraycopy(bytes, bytes.length - 4 - mainlen, maindex, 0, mainlen);
                        byte[] decrypt = AES.decrypt(maindex);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(decrypt);
                        fos.flush();
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }
        List list = new ArrayList<File>();
        Log.d("FAKE",Arrays.toString(app.listFiles()));
        for (File file : app.listFiles()) {
            if (file.getName().equals("classes.dex")) {
                list.add(file);
            }
        }
        Log.d("FAKE",list.toString());
        try {
            V19.install(getClassLoader(), list, unZipFile);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }


    private static Field findField(Object instance, String name) throws NoSuchFieldException {
        Class clazz = instance.getClass();

        while (clazz != null) {
            try {
                Field e = clazz.getDeclaredField(name);
                if (!e.isAccessible()) {
                    e.setAccessible(true);
                }

                return e;
            } catch (NoSuchFieldException var4) {
                clazz = clazz.getSuperclass();
            }
        }

        throw new NoSuchFieldException("Field " + name + " not found in " + instance.getClass());
    }

    private static Method findMethod(Object instance, String name, Class... parameterTypes)
            throws NoSuchMethodException {
        Class clazz = instance.getClass();

        while (clazz != null) {
            try {
                Method e = clazz.getDeclaredMethod(name, parameterTypes);
                if (!e.isAccessible()) {
                    e.setAccessible(true);
                }

                return e;
            } catch (NoSuchMethodException var5) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchMethodException("Method " + name + " with parameters " + Arrays.asList
                (parameterTypes) + " not found in " + instance.getClass());
    }

    private static void expandFieldArray(Object instance, String fieldName, Object[]
            extraElements) throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        Field jlrField = findField(instance, fieldName);
        Object[] original = (Object[]) ((Object[]) jlrField.get(instance));
        Object[] combined = (Object[]) ((Object[]) Array.newInstance(original.getClass()
                .getComponentType(), original.length + extraElements.length));
        System.arraycopy(original, 0, combined, 0, original.length);
        System.arraycopy(extraElements, 0, combined, original.length, extraElements.length);
        jlrField.set(instance, combined);
    }

    private static final class V19 {
        private V19() {
        }

        private static void install(ClassLoader loader, List<File> additionalClassPathEntries,
                                    File optimizedDirectory) throws IllegalArgumentException,
                IllegalAccessException, NoSuchFieldException, InvocationTargetException,
                NoSuchMethodException {

            Field pathListField = findField(loader, "pathList");
            Object dexPathList = pathListField.get(loader);
            ArrayList suppressedExceptions = new ArrayList();
            expandFieldArray(dexPathList, "dexElements", makeDexElements(dexPathList, new
                            ArrayList(additionalClassPathEntries), optimizedDirectory,
                    suppressedExceptions));
            if (suppressedExceptions.size() > 0) {
                Iterator suppressedExceptionsField = suppressedExceptions.iterator();

                while (suppressedExceptionsField.hasNext()) {
                    IOException dexElementsSuppressedExceptions = (IOException)
                            suppressedExceptionsField.next();
                    Log.w("MultiDex", "Exception in makeDexElement",
                            dexElementsSuppressedExceptions);
                }

                Field suppressedExceptionsField1 = findField(loader,
                        "dexElementsSuppressedExceptions");
                IOException[] dexElementsSuppressedExceptions1 = (IOException[]) ((IOException[])
                        suppressedExceptionsField1.get(loader));
                if (dexElementsSuppressedExceptions1 == null) {
                    dexElementsSuppressedExceptions1 = (IOException[]) suppressedExceptions
                            .toArray(new IOException[suppressedExceptions.size()]);
                } else {
                    IOException[] combined = new IOException[suppressedExceptions.size() +
                            dexElementsSuppressedExceptions1.length];
                    suppressedExceptions.toArray(combined);
                    System.arraycopy(dexElementsSuppressedExceptions1, 0, combined,
                            suppressedExceptions.size(), dexElementsSuppressedExceptions1.length);
                    dexElementsSuppressedExceptions1 = combined;
                }

                suppressedExceptionsField1.set(loader, dexElementsSuppressedExceptions1);
            }

        }

        private static Object[] makeDexElements(Object dexPathList, ArrayList<File> files, File
                optimizedDirectory, ArrayList<IOException> suppressedExceptions) throws
                IllegalAccessException, InvocationTargetException, NoSuchMethodException {
            Method makeDexElements = findMethod(dexPathList, "makeDexElements", new
                    Class[]{ArrayList.class, File.class, ArrayList.class});
            return (Object[]) ((Object[]) makeDexElements.invoke(dexPathList, new Object[]{files,
                    optimizedDirectory, suppressedExceptions}));
        }
    }

    private byte[] getBytes(File file) throws Exception {
        RandomAccessFile r = new RandomAccessFile(file, "r");
        byte[] buffer = new byte[(int) r.length()];
        r.readFully(buffer);
        r.close();
        return buffer;
    }
}
