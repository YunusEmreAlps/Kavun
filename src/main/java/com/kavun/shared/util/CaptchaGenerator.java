package com.kavun.shared.util;

import javax.imageio.ImageIO;

import com.kavun.constant.AuthConstants;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;

public class CaptchaGenerator {

  private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
  private static final int WIDTH = 80;
  private static final int HEIGHT = 33;

  public static String generateCode(int length) {
    SecureRandom random = new SecureRandom();
    StringBuilder code = new StringBuilder(length);

    for (int i = 0; i < length; i++) {
      code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
    }
    return code.toString();
  }

  public static String generateImageBase64(String code) {
    BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();

    g2d.setColor(Color.WHITE);
    g2d.fillRect(0, 0, WIDTH, HEIGHT);

    SecureRandom random = new SecureRandom();
    g2d.setColor(Color.LIGHT_GRAY);
    for (int i = 0; i < 15; i++) {
      int x1 = random.nextInt(WIDTH);
      int y1 = random.nextInt(HEIGHT);
      int x2 = random.nextInt(WIDTH);
      int y2 = random.nextInt(HEIGHT);
      g2d.drawLine(x1, y1, x2, y2);
    }

    g2d.setFont(new Font("Arial", Font.BOLD, 18));
    g2d.setColor(Color.BLACK);
    FontMetrics fontMetrics = g2d.getFontMetrics();
    int x = (WIDTH - fontMetrics.stringWidth(code)) / 2;
    int y = ((HEIGHT - fontMetrics.getHeight()) / 2) + fontMetrics.getAscent();
    g2d.drawString(code, x, y);

    g2d.dispose();

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(image, "png", baos);
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (Exception e) {
      throw new RuntimeException(AuthConstants.CAPTCHA_GENERATION_FAILED, e);
    }
  }
}
