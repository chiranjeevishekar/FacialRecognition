package me.chiranjeevi.facialrecognition;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import javax.swing.JOptionPane;
import java.awt.Font;
import java.io.File;
import java.util.Objects;

public class FacialRecognition {
    /**
     * Directory on the disk containing faces
     */
    private static final File DATABASE = new File("Database");

    public static void main(String[] args) {
        OpenCV.loadLocally();
        capture();
        System.exit(0);
    }

    private static void capture() {
        File classifier = new File("lbpcascade_frontalface_improved.xml");

        if (!classifier.exists()) {
            displayFatalError("Unable to find classifier!");
            return;
        }

        CascadeClassifier faceDetector = new CascadeClassifier(classifier.toString());
        VideoCapture camera = new VideoCapture();
        camera.open(0);

        if (!camera.isOpened()) {
            displayFatalError("No camera detected!");
            return;
        }

        if (!DATABASE.exists())
            DATABASE.mkdir();

        ImageFrame frame = new ImageFrame();

        while (frame.isOpen() && camera.isOpened()) {
            Mat rawImage = new Mat();
            camera.read(rawImage);
            Mat newImage = detectFaces(rawImage, faceDetector, frame);
            frame.showImage(newImage);
        }

        camera.release();
    }

    private static Mat detectFaces(Mat image, CascadeClassifier faceDetector, ImageFrame frame) {
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(image, faceDetections);
        Rect[] faces = faceDetections.toArray();
        boolean shouldSave = frame.shouldSave();
        String name = frame.getFileName();
        Scalar color = frame.getTextColor();

        for (Rect face : faces) {
            Mat croppedImage = new Mat(image, face);

            if (shouldSave)
                saveImage(croppedImage, name);

            String id = identifyFace(croppedImage);
            Imgproc.putText(image, "ID: " + id, face.tl(), Font.BOLD, 1.5, color);
            Imgproc.rectangle(image, face.tl(), face.br(), color);
        }

        int faceCount = faces.length;
        String message = faceCount + " face" + (faceCount == 1 ? "" : "s") + " detected!";
        Imgproc.putText(image, message, new Point(3, 25), Font.BOLD, 2, color);

        return image;
    }

    private static String identifyFace(Mat image) {
        int errorThreshold = 3;
        int mostSimilar = -1;
        String mostSimilarID = "???";

        for (File capture : Objects.requireNonNull(DATABASE.listFiles())) {
            int similarities = compareFaces(image, capture.getAbsolutePath());

            if (similarities > mostSimilar) {
                mostSimilar = similarities;
                mostSimilarID = capture.getName().split("\\.")[0]; // Extracting ID from file name
            }
        }

        if (mostSimilar > errorThreshold) {
            return mostSimilarID;
        } else {
            return "???";
        }
    }

    private static int compareFaces(Mat currentImage, String fileName) {
        Mat compareImage = Imgcodecs.imread(fileName);
        ORB orb = ORB.create();
        int similarity = 0;

        MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
        orb.detect(currentImage, keypoints1);
        orb.detect(compareImage, keypoints2);

        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();
        orb.compute(currentImage, keypoints1, descriptors1);
        orb.compute(compareImage, keypoints2, descriptors2);

        if (descriptors1.cols() == descriptors2.cols()) {
            MatOfDMatch matchMatrix = new MatOfDMatch();
            DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING).match(descriptors1, descriptors2, matchMatrix);

            for (DMatch match : matchMatrix.toList()) {
                if (match.distance <= 50) {
                    similarity++;
                }
            }
        }

        return similarity;
    }

    private static void saveImage(Mat image, String name) {
        String extension = ".png";
        String baseName = DATABASE + File.separator + name;
        File basic = new File(baseName + extension);

        if (!basic.exists()) {
            Imgcodecs.imwrite(basic.toString(), image);
        } else {
            int index = 0;
            File destination;
            do
                destination = new File(baseName + " (" + index++ + ")" + extension);
            while (destination.exists());

            Imgcodecs.imwrite(destination.toString(), image);
        }
    }

    private static void displayFatalError(String message) {
        JOptionPane.showMessageDialog(null, message, "Fatal Error", JOptionPane.ERROR_MESSAGE);
    }
}
