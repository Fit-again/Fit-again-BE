package com.fitagain.global.util;

import com.fitagain.domain.recommend.dto.DamageMarkerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * 원본 이미지 위에 손상/변경 부위를 가리키는 번호가 매겨진 원형 마커를 합성합니다.
 */
@Slf4j
@Component
public class ImageMarkerRenderer {

    private static final int CIRCLE_DIAMETER = 44;
    private static final Color CIRCLE_FILL = new Color(230, 57, 70, 220);
    private static final Color CIRCLE_BORDER = Color.WHITE;

    /**
     * 원본 이미지를 다운로드해 마커를 합성한 PNG 바이트를 반환합니다.
     * markers가 비어있으면 원본 이미지를 그대로 PNG로 반환합니다.
     */
    public byte[] renderMarkers(String imageUrl, List<DamageMarkerDto> markers) {
        try {
            BufferedImage original = downloadImage(imageUrl);

            if (markers != null && !markers.isEmpty()) {
                Graphics2D g = original.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setFont(new Font("SansSerif", Font.BOLD, 20));

                for (DamageMarkerDto marker : markers) {
                    drawMarker(g, original.getWidth(), original.getHeight(), marker);
                }

                g.dispose();
            }

            return toPngBytes(original);
        } catch (Exception e) {
            log.error("마커 이미지 합성 실패. imageUrl={}", imageUrl, e);
            throw new IllegalStateException("마커 이미지 합성 실패: " + imageUrl, e);
        }
    }

    private void drawMarker(Graphics2D g, int imageWidth, int imageHeight, DamageMarkerDto marker) {
        int centerX = (int) Math.round(imageWidth * (marker.getXPercent() / 100.0));
        int centerY = (int) Math.round(imageHeight * (marker.getYPercent() / 100.0));
        int radius = CIRCLE_DIAMETER / 2;

        g.setColor(CIRCLE_FILL);
        g.fillOval(centerX - radius, centerY - radius, CIRCLE_DIAMETER, CIRCLE_DIAMETER);

        g.setColor(CIRCLE_BORDER);
        g.setStroke(new BasicStroke(2f));
        g.drawOval(centerX - radius, centerY - radius, CIRCLE_DIAMETER, CIRCLE_DIAMETER);

        String numberText = String.valueOf(marker.getNumber());
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(numberText);
        int textHeight = metrics.getAscent();
        g.drawString(numberText, centerX - textWidth / 2, centerY + textHeight / 2 - 2);
    }

    private BufferedImage downloadImage(String url) throws IOException {
        try (InputStream in = URI.create(url).toURL().openStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("이미지를 디코딩할 수 없습니다: " + url);
            }
            return image;
        }
    }

    private byte[] toPngBytes(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }
}
