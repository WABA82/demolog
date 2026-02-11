package com.examples.demolog.global.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 체크섬 계산 유틸 클래스
 */
public final class ChecksumUtil {

    /**
     * 바이트 배열 SHA-256 체크섬 계산
     */
    public static String getSHA256(byte[] input) {
        return getChecksum(input, "SHA-256");
    }

    /**
     * 지정된 알고리즘으로 문자열의 체크섬 계산
     *
     * @return 16진수 문자열로 변환된 체크섬
     */
    public static String getChecksum(byte[] input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(input);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("지원하지 않는 알고리즘: " + algorithm, e);
        }
    }

    /**
     * 파일의 SHA-256 체크섬 계산
     */
    public static String getFileSHA256(String filePath) {
        return getFileChecksum(filePath, "SHA-256");
    }

    /**
     * 파일의 체크섬 계산
     *
     * @param filePath  파일 경로
     * @param algorithm 해시 알고리즘
     * @return 16진수 문자열로 변환된 체크섬
     */
    public static String getFileChecksum(String filePath, String algorithm) {
        return getFileChecksum(new File(filePath), algorithm);
    }

    /**
     * 파일의 체크섬 계산
     *
     * @param file      파일 객체
     * @param algorithm 해시 알고리즘
     * @return 16진수 문자열로 변환된 체크섬
     */
    public static String getFileChecksum(File file, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hash = digest.digest();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("지원하지 않는 알고리즘: " + algorithm, e);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 읽기 오류: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 두 체크섬 값이 동일한지 비교
     */
    public static boolean verify(String data, String expectedChecksum, String algorithm) {
        String calculatedChecksum = getChecksum(data.getBytes(StandardCharsets.UTF_8), algorithm);
        return calculatedChecksum.equalsIgnoreCase(expectedChecksum);
    }

    /**
     * 파일 체크섬 검증
     */
    public static boolean verifyFile(File file, String expectedChecksum, String algorithm) {
        String calculatedChecksum = getFileChecksum(file, algorithm);
        return calculatedChecksum.equalsIgnoreCase(expectedChecksum);
    }
}