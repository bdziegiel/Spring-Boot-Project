package com.example.demo;

import com.google.gson.JsonObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@RestController
public class ImageController {

    private Image myImage = new Image();

    @RequestMapping("/")
    public String index() {
        return "index";
    }

    //POST
    @RequestMapping(value = "/image", method = RequestMethod.POST)
    public String addImage(HttpServletRequest requestEntity) throws IOException {
        InputStream is = requestEntity.getInputStream();
        BufferedImage bi = ImageIO.read(is);
        JsonObject json = new JsonObject();
        Integer id = myImage.saveImage(bi);
        json.addProperty("id", id);
        json.addProperty("width", bi.getWidth());
        json.addProperty("height", bi.getHeight());
        return json.toString();
    }

    //SHOW
    @RequestMapping(value = "/image/show/{id}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] showImage(@PathVariable Integer id) throws ImageNotFoundException, IOException {
        BufferedImage bi = myImage.getImage(id);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(bi, "jpg", outputStream);
        return outputStream.toByteArray();
    }

    //DELETE
    @RequestMapping("/image/{id}")
    public ResponseEntity<String> deleteImageById(@PathVariable Integer id) {
        try {
            myImage.deleteById(id);
            return new ResponseEntity<>("Deleted", HttpStatus.OK);
        } catch (ImageNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    //GET size
    @RequestMapping("/image/{id}/size")
    public ResponseEntity<String> getImageSize(@PathVariable Integer id) {
        BufferedImage bi = null;
        try {
            bi = myImage.getImage(id);
        } catch (ImageNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("width", bi.getWidth());
        jsonObject.addProperty("height", bi.getHeight());
        return new ResponseEntity<>(jsonObject.toString(), HttpStatus.OK);

    }

    // GET scale
    @RequestMapping(value = "/image/{id}/scale/{percent}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getScaleImage(@PathVariable Integer id, @PathVariable Double percent) throws IOException {
        BufferedImage before = null;
        try {
            before = myImage.getImage(id);
        } catch (ImageNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (percent < 1 || percent > 100){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        int w = (int) (before.getWidth() * (percent / 100));
        int h = (int) (before.getHeight() * (percent / 100));
        BufferedImage after = new BufferedImage(w, h, before.getType());
        AffineTransform at = new AffineTransform();
        at.scale(percent / 100, percent / 100);
        AffineTransformOp atOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
        after = atOp.filter(before, after);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(after, "jpg", bos);
        return new ResponseEntity<>(bos.toByteArray(), HttpStatus.OK);
    }

    // GET histogram
    @RequestMapping("/image/{id}/histogram")
    public ResponseEntity<String> getImageHistogram(@PathVariable Integer id) {
        BufferedImage image = null;
        try {
            image = myImage.getImage(id);
        } catch (ImageNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        int[] red = new int[256];
        int[] green = new int[256];
        int[] blue = new int[256];

        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getHeight(); j++) {
                Color c = new Color(image.getRGB(i, j));
                red[c.getRed()]++;
                green[c.getGreen()]++;
                blue[c.getBlue()]++;
            }
        }
        JsonObject jsonRed = new JsonObject();
        JsonObject jsonGreen = new JsonObject();
        JsonObject jsonBlue = new JsonObject();

        for (int i = 0; i < 255; i++) {
            jsonRed.addProperty("" + i + "", red[i]);
            jsonGreen.addProperty("" + i + "", green[i]);
            jsonBlue.addProperty("" + i + "", blue[i]);
        }
        JsonObject jsonRGB = new JsonObject();
        jsonRGB.add("R", jsonRed);
        jsonRGB.add("G", jsonGreen);
        jsonRGB.add("B", jsonBlue);
        return new ResponseEntity<>(jsonRGB.toString(), HttpStatus.OK);
    }

    // GET crop
    @RequestMapping(value = "/image/{id}/crop/{startX}/{startY}/{width}/{height}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImageFragment(@PathVariable Integer id, @PathVariable Integer startX, @PathVariable Integer startY,
                                                   @PathVariable Integer width, @PathVariable Integer height) throws IOException {
        BufferedImage bi = null;
        try {
            bi = myImage.getImage(id);
        } catch (ImageNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if(bi.getWidth() < (startX+width) || bi.getHeight() < (startY+height)){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        BufferedImage newImage = new BufferedImage(width, height, bi.getType());
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                newImage.setRGB(i, j, bi.getRGB(startX + i, startY + j));
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(newImage, "jpg", bos);
        return new ResponseEntity<>(bos.toByteArray(), HttpStatus.OK);
    }

    // GET greyscale
    @RequestMapping(value = "/image/{id}/greyscale", method = RequestMethod.GET, produces =
            MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImageGrey(@PathVariable Integer id) throws IOException {
        BufferedImage bImage = null;
        try {
            bImage = myImage.getImage(id);
        } catch (ImageNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        BufferedImage newImage = new BufferedImage(bImage.getWidth(),bImage.getHeight(),bImage.getType());
        for (int i = 0; i < newImage.getHeight(); i++) {
            for (int j = 0; j < newImage.getWidth(); j++) {
                Color c = new Color(bImage.getRGB(j, i));
                int red = (int) (c.getRed() * 0.299);
                int green = (int) (c.getGreen() * 0.587);
                int blue = (int) (c.getBlue() * 0.114);
                Color newColor = new Color(
                        red + green + blue,
                        red + green + blue,
                        red + green + blue);

                newImage.setRGB(j, i, newColor.getRGB());
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(newImage, "jpg", bos);
        return new ResponseEntity<>(bos.toByteArray(), HttpStatus.OK);
    }

    // GET blur (rozmycie)
    @RequestMapping(value = "/image/{id}/blur/{radius}", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImageBlur(@PathVariable Integer id, @PathVariable Integer radius) throws IOException {
        BufferedImage image = null;
        try {
            image = myImage.getImage(id);
        } catch (ImageNotFoundException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); //404
        }
        if (radius <= 0){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); //400
        }
        int rad2 = radius * radius;
        float[] matrix = new float[rad2];
        for (int i = 0; i < rad2; i++)
            matrix[i] = 1.0f / (float) rad2;
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        BufferedImageOp op = new ConvolveOp(new Kernel(radius, radius, matrix), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage blurredImage = op.filter(image, newImage);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(blurredImage, "jpg", bos);
        return new ResponseEntity<>(bos.toByteArray(), HttpStatus.OK);
    }
}