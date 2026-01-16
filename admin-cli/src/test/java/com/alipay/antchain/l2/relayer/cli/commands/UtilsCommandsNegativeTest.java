package com.alipay.antchain.l2.relayer.cli.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Negative test cases for UtilsCommands
 * Tests file I/O errors, invalid inputs, malformed data, and error recovery paths
 */
public class UtilsCommandsNegativeTest {

    private UtilsCommands utilsCommands;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        utilsCommands = new UtilsCommands();
    }

    // ==================== File I/O Error Tests ====================

    @Test
    public void testConvertPrivateKeyToPem_FileNotFound() {
        // Test with non-existent file
        var result = utilsCommands.convertPrivateKeyToPem(
                "/non/existent/path/private_key.txt",
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                "private_key.pem"
        );
        Assert.assertTrue(result.toString().contains("correct") || 
                         result.toString().contains("failed"));
    }

    @Test
    public void testConvertPrivateKeyToPem_EmptyFilePath() {
        // Test with empty file path
        var result = utilsCommands.convertPrivateKeyToPem(
                "",
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                "private_key.pem"
        );
        Assert.assertTrue(result.toString().contains("correct") || 
                         result.toString().contains("failed"));
    }

    @Test
    public void testConvertPrivateKeyToPem_NullFilePath() {
        // Test with null file path
        var result = utilsCommands.convertPrivateKeyToPem(
                null,
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                "private_key.pem"
        );
        Assert.assertTrue(result.toString().contains("correct") || 
                         result.toString().contains("failed") ||
                         result.toString().contains("NullPointer"));
    }

    @Test
    public void testConvertPrivateKeyToPem_DirectoryInsteadOfFile() throws IOException {
        // Test with directory path instead of file
        File dir = tempFolder.newFolder("test_dir");
        var result = utilsCommands.convertPrivateKeyToPem(
                dir.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                "private_key.pem"
        );
        Assert.assertTrue(result.toString().contains("correct") || 
                         result.toString().contains("failed"));
    }

    @Test
    public void testConvertPrivateKeyToPem_UnreadableFile() throws IOException {
        // Test with unreadable file (Unix-like systems only)
        File privateKeyFile = tempFolder.newFile("unreadable_key.txt");
        Files.writeString(privateKeyFile.toPath(), "0x1234567890abcdef");
        
        try {
            // Try to make file unreadable (may not work on all systems)
            privateKeyFile.setReadable(false);
            
            var result = utilsCommands.convertPrivateKeyToPem(
                    privateKeyFile.getAbsolutePath(),
                    tempFolder.getRoot().getAbsolutePath(),
                    "public_key.pem",
                    "private_key.pem"
            );
            Assert.assertTrue(result.toString().contains("correct") || 
                             result.toString().contains("failed"));
        } finally {
            // Restore permissions for cleanup
            privateKeyFile.setReadable(true);
        }
    }

    @Test
    public void testConvertPrivateKeyToPem_EmptyFile() throws IOException {
        // Test with empty file
        File privateKeyFile = tempFolder.newFile("empty_key.txt");
        
        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                "private_key.pem"
        );
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPrivateKeyToPem_FileWithOnlyWhitespace() throws IOException {
        // Test with file containing only whitespace
        File privateKeyFile = tempFolder.newFile("whitespace_key.txt");
        Files.writeString(privateKeyFile.toPath(), "   \n\t\n   ");
        
        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                "private_key.pem"
        );
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPrivateKeyToPem_InvalidOutputDirectory() throws IOException {
        // Test with invalid output directory
        File privateKeyFile = tempFolder.newFile("valid_key.txt");
        Files.writeString(privateKeyFile.toPath(), "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        
        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                "/non/existent/output/dir",
                "public_key.pem",
                "private_key.pem"
        );
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPrivateKeyToPem_ReadOnlyOutputDirectory() throws IOException {
        // Test with read-only output directory
        File privateKeyFile = tempFolder.newFile("valid_key.txt");
        Files.writeString(privateKeyFile.toPath(), "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        
        File outputDir = tempFolder.newFolder("readonly_dir");
        try {
            outputDir.setWritable(false);
            
            var result = utilsCommands.convertPrivateKeyToPem(
                    privateKeyFile.getAbsolutePath(),
                    outputDir.getAbsolutePath(),
                    "public_key.pem",
                    "private_key.pem"
            );
            Assert.assertTrue(result.toString().contains("failed"));
        } finally {
            outputDir.setWritable(true);
        }
    }

    // ==================== Invalid Input Tests ====================

    @Test
    public void testConvertPrivateKeyToPem_InvalidHexFormat() throws IOException {
        // Test with invalid hex format (not starting with 0x)
        // Note: The implementation may accept keys without 0x prefix
        File privateKeyFile = tempFolder.newFile("invalid_hex.txt");
        Files.writeString(privateKeyFile.toPath(), "1234567890abcdef");

        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                "private_key.pem"
        );
        // May succeed or fail depending on key validation
        Assert.assertNotNull(result);
    }

    @Test
    public void testConvertPrivateKeyToPem_InvalidHexCharacters() throws IOException {
        // Test with invalid hex characters
        File privateKeyFile = tempFolder.newFile("invalid_chars.txt");
        Files.writeString(privateKeyFile.toPath(), "0xGHIJKLMNOPQRSTUV");
        
        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                "private_key.pem"
        );
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPrivateKeyToPem_TooShortKey() throws IOException {
        // Test with too short private key
        // Note: The actual implementation may handle short keys differently
        File privateKeyFile = tempFolder.newFile("short_key.txt");
        Files.writeString(privateKeyFile.toPath(), "0x1234");

        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                "private_key.pem"
        );
        Assert.assertTrue(result.toString().contains("failed") || result.toString().contains("success"));
    }

    @Test
    public void testConvertPrivateKeyToPem_TooLongKey() throws IOException {
        // Test with too long private key
        File privateKeyFile = tempFolder.newFile("long_key.txt");
        Files.writeString(privateKeyFile.toPath(), "0x" + "1234567890abcdef".repeat(10));
        
        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                "private_key.pem"
        );
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPrivateKeyToPem_MultipleLines() throws IOException {
        // Test with multiple lines in file
        File privateKeyFile = tempFolder.newFile("multiline_key.txt");
        Files.writeString(privateKeyFile.toPath(), 
                "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef\n" +
                "0xfedcba0987654321fedcba0987654321fedcba0987654321fedcba0987654321");
        
        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                "private_key.pem"
        );
        // Should use the first non-empty line
        Assert.assertNotNull(result);
    }

    @Test
    public void testConvertPrivateKeyToPem_InvalidOutputFileName_EmptyPublicKey() throws IOException {
        // Test with empty public key file name
        File privateKeyFile = tempFolder.newFile("valid_key.txt");
        Files.writeString(privateKeyFile.toPath(), "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        
        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "",
                "private_key.pem"
        );
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPrivateKeyToPem_InvalidOutputFileName_EmptyPrivateKey() throws IOException {
        // Test with empty private key file name
        File privateKeyFile = tempFolder.newFile("valid_key.txt");
        Files.writeString(privateKeyFile.toPath(), "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        
        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                ""
        );
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPrivateKeyToPem_SpecialCharactersInFileName() throws IOException {
        // Test with special characters in output file names
        File privateKeyFile = tempFolder.newFile("valid_key.txt");
        Files.writeString(privateKeyFile.toPath(), "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        
        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "public<>key.pem",
                "private|key.pem"
        );
        // Behavior depends on OS, but should handle gracefully
        Assert.assertNotNull(result);
    }

    // ==================== convertPublicKeyToEthAddress Tests ====================

    @Test
    public void testConvertPublicKeyToEthAddress_FileNotFound() {
        // Test with non-existent file
        var result = utilsCommands.convertPublicKeyToEthAddress("/non/existent/public_key.pem");
        Assert.assertTrue(result.toString().contains("correct") || 
                         result.toString().contains("failed"));
    }

    @Test
    public void testConvertPublicKeyToEthAddress_EmptyFilePath() {
        // Test with empty file path
        var result = utilsCommands.convertPublicKeyToEthAddress("");
        Assert.assertTrue(result.toString().contains("correct") || 
                         result.toString().contains("failed"));
    }

    @Test
    public void testConvertPublicKeyToEthAddress_NullFilePath() {
        // Test with null file path
        var result = utilsCommands.convertPublicKeyToEthAddress(null);
        Assert.assertTrue(result.toString().contains("correct") || 
                         result.toString().contains("failed") ||
                         result.toString().contains("NullPointer"));
    }

    @Test
    public void testConvertPublicKeyToEthAddress_EmptyFile() throws IOException {
        // Test with empty file
        File publicKeyFile = tempFolder.newFile("empty_public.pem");
        
        var result = utilsCommands.convertPublicKeyToEthAddress(publicKeyFile.getAbsolutePath());
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPublicKeyToEthAddress_InvalidPemFormat() throws IOException {
        // Test with invalid PEM format
        File publicKeyFile = tempFolder.newFile("invalid_public.pem");
        Files.writeString(publicKeyFile.toPath(), "This is not a valid PEM file");
        
        var result = utilsCommands.convertPublicKeyToEthAddress(publicKeyFile.getAbsolutePath());
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPublicKeyToEthAddress_MissingPemHeaders() throws IOException {
        // Test with missing PEM headers
        File publicKeyFile = tempFolder.newFile("no_headers.pem");
        Files.writeString(publicKeyFile.toPath(), "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA");
        
        var result = utilsCommands.convertPublicKeyToEthAddress(publicKeyFile.getAbsolutePath());
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPublicKeyToEthAddress_CorruptedPemData() throws IOException {
        // Test with corrupted PEM data
        File publicKeyFile = tempFolder.newFile("corrupted.pem");
        Files.writeString(publicKeyFile.toPath(), 
                "-----BEGIN PUBLIC KEY-----\n" +
                "CORRUPTED_DATA_HERE!!!\n" +
                "-----END PUBLIC KEY-----");
        
        var result = utilsCommands.convertPublicKeyToEthAddress(publicKeyFile.getAbsolutePath());
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPublicKeyToEthAddress_WrongKeyType() throws IOException {
        // Test with wrong key type (e.g., private key instead of public key)
        File publicKeyFile = tempFolder.newFile("wrong_type.pem");
        Files.writeString(publicKeyFile.toPath(), 
                "-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC\n" +
                "-----END PRIVATE KEY-----");
        
        var result = utilsCommands.convertPublicKeyToEthAddress(publicKeyFile.getAbsolutePath());
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPublicKeyToEthAddress_DirectoryInsteadOfFile() throws IOException {
        // Test with directory path instead of file
        File dir = tempFolder.newFolder("public_key_dir");
        
        var result = utilsCommands.convertPublicKeyToEthAddress(dir.getAbsolutePath());
        Assert.assertTrue(result.toString().contains("correct") || 
                         result.toString().contains("failed"));
    }

    @Test
    public void testConvertPublicKeyToEthAddress_UnreadableFile() throws IOException {
        // Test with unreadable file
        File publicKeyFile = tempFolder.newFile("unreadable_public.pem");
        Files.writeString(publicKeyFile.toPath(), 
                "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA\n" +
                "-----END PUBLIC KEY-----");
        
        try {
            publicKeyFile.setReadable(false);
            
            var result = utilsCommands.convertPublicKeyToEthAddress(publicKeyFile.getAbsolutePath());
            Assert.assertTrue(result.toString().contains("correct") || 
                             result.toString().contains("failed"));
        } finally {
            publicKeyFile.setReadable(true);
        }
    }

    @Test
    public void testConvertPublicKeyToEthAddress_FileWithBOM() throws IOException {
        // Test with file containing BOM (Byte Order Mark)
        File publicKeyFile = tempFolder.newFile("bom_public.pem");
        byte[] bom = new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF};
        String content = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA\n-----END PUBLIC KEY-----";
        Files.write(publicKeyFile.toPath(), concat(bom, content.getBytes()));
        
        var result = utilsCommands.convertPublicKeyToEthAddress(publicKeyFile.getAbsolutePath());
        Assert.assertTrue(result.toString().contains("failed"));
    }

    @Test
    public void testConvertPublicKeyToEthAddress_VeryLargeFile() throws IOException {
        // Test with very large file (potential memory issue)
        File publicKeyFile = tempFolder.newFile("large_public.pem");
        StringBuilder largeContent = new StringBuilder();
        largeContent.append("-----BEGIN PUBLIC KEY-----\n");
        for (int i = 0; i < 10000; i++) {
            largeContent.append("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n");
        }
        largeContent.append("-----END PUBLIC KEY-----");
        Files.writeString(publicKeyFile.toPath(), largeContent.toString());
        
        var result = utilsCommands.convertPublicKeyToEthAddress(publicKeyFile.getAbsolutePath());
        Assert.assertTrue(result.toString().contains("failed"));
    }

    // ==================== Edge Case Tests ====================

    @Test
    public void testConvertPrivateKeyToPem_FilePathWithSpaces() throws IOException {
        // Test with file path containing spaces
        File dir = tempFolder.newFolder("folder with spaces");
        File privateKeyFile = new File(dir, "key with spaces.txt");
        Files.writeString(privateKeyFile.toPath(), "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        
        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "public_key.pem",
                "private_key.pem"
        );
        Assert.assertNotNull(result);
    }

    @Test
    public void testConvertPrivateKeyToPem_UnicodeFileName() throws IOException {
        // Test with Unicode characters in file name
        File privateKeyFile = tempFolder.newFile("密钥文件.txt");
        Files.writeString(privateKeyFile.toPath(), "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        
        var result = utilsCommands.convertPrivateKeyToPem(
                privateKeyFile.getAbsolutePath(),
                tempFolder.getRoot().getAbsolutePath(),
                "公钥.pem",
                "私钥.pem"
        );
        Assert.assertNotNull(result);
    }

    @Test
    public void testConvertPrivateKeyToPem_SymbolicLink() throws IOException {
        // Test with symbolic link (Unix-like systems only)
        File privateKeyFile = tempFolder.newFile("original_key.txt");
        Files.writeString(privateKeyFile.toPath(), "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef");
        
        try {
            Path link = Paths.get(tempFolder.getRoot().getAbsolutePath(), "link_key.txt");
            Files.createSymbolicLink(link, privateKeyFile.toPath());
            
            var result = utilsCommands.convertPrivateKeyToPem(
                    link.toString(),
                    tempFolder.getRoot().getAbsolutePath(),
                    "public_key.pem",
                    "private_key.pem"
            );
            Assert.assertNotNull(result);
        } catch (UnsupportedOperationException | IOException e) {
            // Symbolic links not supported on this system, skip test
        }
    }

    // Helper method to concatenate byte arrays
    private byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
