package org.asamk.signal.manager;

import org.signal.zkgroup.ServerPublicParams;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.account.AccountAttributes;
import org.whispersystems.signalservice.api.push.TrustStore;
import org.whispersystems.signalservice.internal.configuration.SignalCdnUrl;
import org.whispersystems.signalservice.internal.configuration.SignalContactDiscoveryUrl;
import org.whispersystems.signalservice.internal.configuration.SignalKeyBackupServiceUrl;
import org.whispersystems.signalservice.internal.configuration.SignalServiceConfiguration;
import org.whispersystems.signalservice.internal.configuration.SignalServiceUrl;
import org.whispersystems.signalservice.internal.configuration.SignalStorageUrl;
import org.whispersystems.util.Base64;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import okhttp3.Dns;
import okhttp3.Interceptor;

public class ServiceConfig {

    private final static String SIGNAL_ORG_UNIDENTIFIED_SENDER_TRUST_ROOT = "BXu6QIKVz5MA8gstzfOgRQGqyLqOwNKHL6INkv3IHWMF";
    static String UNIDENTIFIED_SENDER_TRUST_ROOT = SIGNAL_ORG_UNIDENTIFIED_SENDER_TRUST_ROOT;
    final static int PREKEY_MINIMUM_COUNT = 20;
    final static int PREKEY_BATCH_SIZE = 100;
    final static int MAX_ATTACHMENT_SIZE = 150 * 1024 * 1024;
    final static int MAX_ENVELOPE_SIZE = 0;
    final static long AVATAR_DOWNLOAD_FAILSAFE_MAX_SIZE = 10 * 1024 * 1024;

    
    private final static String SIGNAL_ORG_CDS_MRENCLAVE = "c98e00a4e3ff977a56afefe7362a27e4961e4f19e211febfbb19b897e6b80b15";
    static String CDS_MRENCLAVE = SIGNAL_ORG_CDS_MRENCLAVE;

    private final static String SIGNAL_ORG_URL = "https://textsecure-service.whispersystems.org";
    private final static String SIGNAL_ORG_CDN_URL = "https://cdn.signal.org";
    private final static String SIGNAL_ORG_CDN2_URL = "https://cdn2.signal.org";
    private final static String SIGNAL_ORG_SIGNAL_CONTACT_DISCOVERY_URL = "https://api.directory.signal.org";
    private final static String SIGNAL_ORG_SIGNAL_KEY_BACKUP_URL = "https://api.backup.signal.org";
    private final static String SIGNAL_ORG_STORAGE_URL = "https://storage.signal.org";
    private final static String SIGNAL_ORG_zkGroupServerPublicParamsHex = "AMhf5ywVwITZMsff/eCyudZx9JDmkkkbV6PInzG4p8x3VqVJSFiMvnvlEKWuRob/1eaIetR31IYeAbm0NdOuHH8Qi+Rexi1wLlpzIo1gstHWBfZzy1+qHRV5A4TqPp15YzBPm0WSggW6PbSn+F4lf57VCnHF7p8SvzAA2ZZJPYJURt8X7bbg+H3i+PEjH9DXItNEqs2sNcug37xZQDLm7X0=";

    private final static TrustStore TRUST_STORE = new WhisperTrustStore();
    private final static TrustStore IAS_TRUST_STORE = new IasTrustStore();

    private final static Optional<Dns> dns = Optional.absent();

    private static byte[] zkGroupServerPublicParams;

    static AccountAttributes.Capabilities capabilities;

    public static SignalServiceConfiguration createDefaultServiceConfiguration(String userAgent, Properties props) {
        final Interceptor userAgentInterceptor = chain -> chain.proceed(chain.request()
                .newBuilder()
                .header("User-Agent", userAgent)
                .build());

        final List<Interceptor> interceptors = Collections.singletonList(userAgentInterceptor);

        UNIDENTIFIED_SENDER_TRUST_ROOT = props.getProperty("UNIDENTIFIED_SENDER_TRUST_ROOT", SIGNAL_ORG_UNIDENTIFIED_SENDER_TRUST_ROOT);
        CDS_MRENCLAVE = props.getProperty("CDS_MRENCLAVE", SIGNAL_ORG_CDS_MRENCLAVE);
        
        String zkGroupServerPublicParamsHexProperty = props.getProperty("zkGroupServerPublicParamsHex", SIGNAL_ORG_zkGroupServerPublicParamsHex);
        try {
            zkGroupServerPublicParams = Base64.decode(zkGroupServerPublicParamsHexProperty);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        boolean zkGroupAvailable;
        try {
            new ServerPublicParams(zkGroupServerPublicParams);
            zkGroupAvailable = true;

        } catch (Throwable ignored) {
            zkGroupAvailable = false;
        }
        capabilities = new AccountAttributes.Capabilities(false, zkGroupAvailable, false, zkGroupAvailable);


        SignalServiceConfiguration signalServiceConfiguration;
        if (props.getProperty("URL") != null) {
            // Custom
            signalServiceConfiguration = new SignalServiceConfiguration(new SignalServiceUrl[]{new SignalServiceUrl(props.getProperty("URL"), null)},
                    makeSignalCdnUrlMapFor(new SignalCdnUrl[]{new SignalCdnUrl(props.getProperty("CDN_URL"), null)}, 
                    new SignalCdnUrl[]{new SignalCdnUrl(props.getProperty("CDN2_URL"), null)}),
            
                    new SignalContactDiscoveryUrl[]{new SignalContactDiscoveryUrl(props.getProperty("SIGNAL_CONTACT_DISCOVERY_URL"), null)},
                    new SignalKeyBackupServiceUrl[]{new SignalKeyBackupServiceUrl(props.getProperty("SIGNAL_KEY_BACKUP_URL"), null)},

                    new SignalStorageUrl[]{new SignalStorageUrl(props.getProperty("STORAGE_URL"), null)},

                    interceptors,
                    dns,
                    zkGroupServerPublicParams);
        }
        else {
            signalServiceConfiguration = new SignalServiceConfiguration(new SignalServiceUrl[]{new SignalServiceUrl(SIGNAL_ORG_URL, TRUST_STORE)},
            makeSignalCdnUrlMapFor(new SignalCdnUrl[]{new SignalCdnUrl(SIGNAL_ORG_CDN_URL, TRUST_STORE)},
                    new SignalCdnUrl[]{new SignalCdnUrl(SIGNAL_ORG_CDN2_URL, TRUST_STORE)}),
            
                    new SignalContactDiscoveryUrl[]{new SignalContactDiscoveryUrl(SIGNAL_ORG_SIGNAL_CONTACT_DISCOVERY_URL, TRUST_STORE)},
                    new SignalKeyBackupServiceUrl[]{new SignalKeyBackupServiceUrl(SIGNAL_ORG_SIGNAL_KEY_BACKUP_URL, TRUST_STORE)},

                    new SignalStorageUrl[]{new SignalStorageUrl(props.getProperty("STORAGE_URL", SIGNAL_ORG_STORAGE_URL), TRUST_STORE)},

                    interceptors,
                    dns,
                    zkGroupServerPublicParams);
        }
        return signalServiceConfiguration;
    }

    public static AccountAttributes.Capabilities getCapabilities() {
        return capabilities;
    }

    static KeyStore getIasKeyStore() {
        try {
            TrustStore contactTrustStore = IAS_TRUST_STORE;

            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(contactTrustStore.getKeyStoreInputStream(),
                    contactTrustStore.getKeyStorePassword().toCharArray());

            return keyStore;
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static Map<Integer, SignalCdnUrl[]> makeSignalCdnUrlMapFor(
            SignalCdnUrl[] cdn0Urls, SignalCdnUrl[] cdn2Urls
    ) {
        return Map.of(0, cdn0Urls, 2, cdn2Urls);
    }

    private ServiceConfig() {
    }
}
