package com.alipay.antchain.l2.relayer.cli.commands;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.l2.relayer.commons.utils.Utils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.shell.standard.*;

@Getter
@ShellCommandGroup(value = "Commands about utils functions")
@ShellComponent
@Slf4j
public class UtilsCommands {

    @ShellMethod(value = "Convert the hex private key to x509 in pem format")
    public Object convertPrivateKeyToPem(
            @ShellOption(help = "Private key file", valueProvider = FileValueProvider.class) String privateKeyFile,
            @ShellOption(valueProvider = FileValueProvider.class, help = "Directory to save the x509 keys", defaultValue = "") String outputDir,
            @ShellOption(help = "File to save the public key", defaultValue = "public_key.pem") String publicKeyPemFile,
            @ShellOption(help = "File to save the private key", defaultValue = "private_key.pem") String privateKeyPemFile
    ) {
        try {
            KeyPair keyPair;
            var privateKeyFilePath = Paths.get(privateKeyFile);
            if (Files.isReadable(privateKeyFilePath)) {
                keyPair = Utils.convertKeyPair(Files.readAllLines(privateKeyFilePath).stream().filter(StrUtil::isNotEmpty).findFirst().orElseThrow());
            } else {
                return "please input the path to the correct applicant public key file";
            }
            var privatePath = Paths.get(outputDir, privateKeyPemFile);
            writePrivateKey(keyPair.getPrivate(), privatePath);

            var publicPath = Paths.get(outputDir, publicKeyPemFile);
            writePublicKey(keyPair.getPublic(), publicPath);

            return "success";
        } catch (Exception e) {
            return "failed: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Convert the pem public key to ethereum address")
    public Object convertPublicKeyToEthAddress(
            @ShellOption(help = "Public key file", valueProvider = FileValueProvider.class) String publicKeyFile
    ) {
        try {
            var publicKeyFilePath = Paths.get(publicKeyFile);
            if (Files.isReadable(publicKeyFilePath)) {
                return Utils.convertPublicKeyToEthAddress(Files.readString(publicKeyFilePath));
            }
            return "please input the path to the correct applicant public key file";
        } catch (Exception e) {
            return "failed: " + e.getMessage();
        }
    }

    @SneakyThrows
    private void writePrivateKey(PrivateKey privateKey, Path outputFile) {
        // dump the private key into pem
        StringWriter stringWriter = new StringWriter(256);
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(privateKey);
        jcaPEMWriter.close();
        String privatePem = stringWriter.toString();
        Files.write(outputFile, privatePem.getBytes());
    }

    @SneakyThrows
    private void writePublicKey(PublicKey publicKey, Path outputFile) {
        // dump the public key into pem
        StringWriter stringWriter = new StringWriter(256);
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(publicKey);
        jcaPEMWriter.close();
        String pubkeyPem = stringWriter.toString();
        Files.write(outputFile, pubkeyPem.getBytes());
    }
}
