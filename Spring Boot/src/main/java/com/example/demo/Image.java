package com.example.demo;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Image  {
    private Map<Integer, BufferedImage> Images;
    private static Integer id;

    public Image(){
        Images = new HashMap<>();
        id = 0;
    }
    public Integer saveImage(BufferedImage bufferedImage){
        Images.put(id,bufferedImage);
        return id++;
    }
    public void deleteById(Integer id) throws ImageNotFoundException{
        if (Images.remove(id) == null) {
            throw new ImageNotFoundException();
        }
    }
    public BufferedImage getImage(Integer id) throws ImageNotFoundException{
        if (Images.get(id) == null) {
            throw new ImageNotFoundException();
        }
        return Images.get(id);
    }


}
