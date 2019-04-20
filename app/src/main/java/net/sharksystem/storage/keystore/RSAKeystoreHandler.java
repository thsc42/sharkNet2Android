package net.sharksystem.storage.keystore;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.spec.MGF1ParameterSpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.security.auth.x500.X500Principal;

import timber.log.Timber;

public final class RSAKeystoreHandler implements KeystoreHandler {

    private static RSAKeystoreHandler rsaKeystoreHandler = null;

    public final static int ANY_PURPOSE = KeyProperties.PURPOSE_ENCRYPT |
            KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_SIGN |
            KeyProperties.PURPOSE_VERIFY;

    private static final String TAG = "RSAKeystoreHandler";

    private static KeyStore keyStore;

    /**
     * KeySize has to be 2048 according to https://developer.android.com/training/articles/keystore#HardwareSecurityModule
     */
    private static final int KEY_SIZE = 2048;

    private static final int KEY_DURATION_YEARS = 10;

    private final static String KEY_ALIAS = "KeyPair";

    private final static String AndroidKeyStore = "AndroidKeyStore";

    private RSAKeystoreHandler() {
        setupKeystore();
    }

    public static RSAKeystoreHandler getInstance() {
        if (rsaKeystoreHandler == null) {
            rsaKeystoreHandler = new RSAKeystoreHandler();
        }
        return rsaKeystoreHandler;
    }

    public void setupKeystore() {
        try {
            keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);
            if (checkRSAKeyPairAvailable()) {
                generateRSAKeyPair();
            }
        } catch (Exception e) {
            Log.d(TAG, "setupKeystore: " + e.getMessage());
        }
    }

    private boolean checkRSAKeyPairAvailable() {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                return true;
            } else {
                return false;
            }
        } catch (KeyStoreException e) {
            Timber.e(e);
        }
        return false;
    }

    private void resetKeystore() {
        try {
            keyStore.deleteEntry(KEY_ALIAS);
            generateRSAKeyPair();
        } catch (KeyStoreException e) {
            Timber.e(e);
        }
    }

    private void generateRSAKeyPair() {
        try {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            //Jahr von heute plus YEAR Jahre
            end.add(Calendar.YEAR, KEY_DURATION_YEARS);

            final long now = java.lang.System.currentTimeMillis();
            final long validityDays = 10000L;


            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            keyPairGenerator.initialize(
                    new KeyGenParameterSpec.Builder(KEY_ALIAS, ANY_PURPOSE)
                            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)

                            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PSS)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)

//                            .setCertificateSubject(new X500Principal("CN=Android, O=Android Authority"))
//                            .setCertificateSerialNumber(new BigInteger(256, new Random()))
//                            .setCertificateNotBefore(new Date(now - (now % 1000L)))
//                            .setCertificateNotAfter(new Date(((new Date(now - (now % 1000L))).getTime()) + (validityDays * 86400000L)))

                            .setUserAuthenticationRequired(false)
                            .setKeyValidityStart(start.getTime())
                            .setKeyValidityEnd(end.getTime())
                            .setKeySize(KEY_SIZE)
                            .build());
            keyPairGenerator.generateKeyPair();

        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "generateRSAKeyPair: " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            Log.d(TAG, "generateRSAKeyPair: " + e.getMessage());
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            Log.d(TAG, "generateRSAKeyPair: " + e.getMessage());
        }
    }

    public byte[] decrypt(byte[] toDecrypt) {
        byte[] decryptedBytes = null;

        try {
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
            OAEPParameterSpec sp = new OAEPParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT);
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey, sp);

            decryptedBytes = cipher.doFinal(toDecrypt);

        } catch (Exception e) {
            Log.d(TAG, "decrypt: Exception occured: " + e.getMessage());

            if (e instanceof UnrecoverableKeyException) {
                resetKeystore();
                Log.d(TAG, "Keystore reset due to unrecoverable Key");
            }
        }

        return decryptedBytes;
    }

    public byte[] encrypt(byte[] toEncrypt) {
        byte[] encryptedBytes = null;

        try {

            PublicKey publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
            OAEPParameterSpec sp = new OAEPParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT);
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, sp);

            encryptedBytes = cipher.doFinal(toEncrypt);

        } catch (Exception e) {
            Timber.d(e, "encrypt: Exception occurred: %s", e.getMessage());
        }
        return encryptedBytes;
    }

    public PublicKey getPublicKey() {
        PublicKey publicKey = null;
        try {
            publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        PrivateKey privateKey = null;
        try {
            privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
            return privateKey;
        } catch (Exception e) {
            Log.d(TAG, "getPrivateKey: " + e.getMessage());

        }
        return privateKey;
    }

}