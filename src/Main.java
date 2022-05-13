// Antonio Fernandez
// 301393610
// CMPT 365 - Project 2

package src;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.*;
import java.nio.*;
import java.nio.file.DirectoryIteratorException;
import java.util.ArrayList;
import javax.imageio.*;
import javax.security.auth.SubjectDomainCombiner;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.*;

import src.LinesComponent;

public class Main {
    public static void main(String args[]){
        JFrame frame = new JFrame("CMPT 365 Project 2 - Home");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton b1 = new JButton("Part 1 (WAV)");
        JButton b2 = new JButton("Part 2 (PNG)");
        b1.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Select File
                JFileChooser files = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                int ret = files.showOpenDialog(null);

                if (ret == JFileChooser.APPROVE_OPTION) {
                    File selected = files.getSelectedFile();
                    part1(selected, frame);   
                }
            }

        });

        b2.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Select File
                JFileChooser files = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                int ret = files.showOpenDialog(null);

                if (ret == JFileChooser.APPROVE_OPTION) {
                    File selected = files.getSelectedFile();
                    part2(selected, frame);   
                }
            }

        });

        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new GridLayout(2,2));

        JLabel select = new JLabel();
        select.setText("Select File:");

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new FlowLayout());
        buttonsPanel.add(b1);
        buttonsPanel.add(b2);

        JButton exitButton = new JButton("Exit Application");
        exitButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }

        });
        exitButton.setBackground(Color.red);

        mainContainer.add(select);
        mainContainer.add(buttonsPanel);
        mainContainer.add(exitButton);

        frame.add(mainContainer);
        frame.setLayout(new FlowLayout());
        frame.pack();
        frame.setVisible(true);
    } // main



    public static void part1(File file, JFrame prevFrame) {
        try {
            prevFrame.setVisible(false);

            // Setup window
            int width = 1000;
            int height = 150;

            JFrame frame = new JFrame(file.getName() + " - Waveforms");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 700);
            frame.setBackground(Color.black);

            LinesComponent linesDisplay1 = new LinesComponent();
            linesDisplay1.setPreferredSize(new Dimension(width, height));
            LinesComponent linesDisplay2 = new LinesComponent();
            linesDisplay2.setPreferredSize(new Dimension(width, height));

            // Grab data
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
            float bitsPerSample = inputStream.getFormat().getSampleSizeInBits();
            long bytesPerFrame = inputStream.getFormat().getFrameSize();
            long numFrames = inputStream.getFrameLength();
            float freq = inputStream.getFormat().getSampleRate();

            System.out.println(bitsPerSample);
            System.out.println(bytesPerFrame);
            
            // Read blockSize number of samples in each channel
            // Draw lines according to local mins and maxes 
            int blockSize = Math.round((int)numFrames / width);             // Try to do resizable window?
            int lineMaxLength = Math.round(height / 2);
            int channel1_xAxis = lineMaxLength;
            int channel2_xAxis = lineMaxLength;
            int x = 0;
            
            int size = (int)(numFrames*bytesPerFrame);
            byte[] audioBytes = new byte[size];

            ByteBuffer converter = ByteBuffer.allocate(2);
            converter.order(ByteOrder.LITTLE_ENDIAN);

            byte zeroByte = 0;
            short value = 0;
            short min1 = 0;
            short max1 = 0;
            short min2 = 0;
            short max2 = 0;
            int globalAbsMaxMag = 32767;     // Max value of SHORT 
            int byteIter = 0;
            int lineLength = 0;

            inputStream.read(audioBytes);
            for (int i = 0; i < numFrames; i++) {
                byteIter = i * (int)bytesPerFrame;

                if (i % blockSize == 0 || i == audioBytes.length-1) {
                    // Add new bar
                    // Will do 2 lines per bar, both coming from the channel's respective x-axes
                    
                    lineLength = Math.abs((int)(lineMaxLength * ((float)(min1) / globalAbsMaxMag)));
                    linesDisplay1.insertLine(x, channel1_xAxis, x, channel1_xAxis+lineLength);
                    lineLength = Math.abs((int)(lineMaxLength * ((float)(max1) / globalAbsMaxMag)));
                    linesDisplay1.insertLine(x, channel1_xAxis, x, channel1_xAxis-lineLength);

                    lineLength = Math.abs((int)(lineMaxLength * ((float)(min2) / globalAbsMaxMag)));
                    linesDisplay2.insertLine(x, channel2_xAxis, x, channel2_xAxis+lineLength);
                    lineLength = Math.abs((int)(lineMaxLength * ((float)(max2) / globalAbsMaxMag)));
                    linesDisplay2.insertLine(x, channel2_xAxis, x, channel2_xAxis-lineLength);
                    
                    // Reset min and max
                    min1 = 0;
                    max1 = 0;
                    min2 = 0;
                    max2 = 0;
                    x++;
                }

                // First channel
                if (bitsPerSample == 8) {
                    converter.put(zeroByte);
                    converter.put(audioBytes[byteIter]);
                } else if (bitsPerSample == 16) {
                    converter.put(audioBytes[byteIter]);              // Read 2 bytes as a short trick
                    converter.put(audioBytes[byteIter+1]);
                }

                value = converter.getShort(0);
                if (value < min1) min1 = value;
                if (value > max1) max1 = value;
                
                converter.clear();

                // Second channel
                if (bitsPerSample == 8) {
                    converter.put(zeroByte);
                    converter.put(audioBytes[byteIter+1]);
                } else if (bitsPerSample == 16) {
                    converter.put(audioBytes[byteIter+2]);
                    converter.put(audioBytes[byteIter+3]);
                }

                // Add this value to my short input stream (For compression)
                value = converter.getShort(0);
                if (value < min2) min2 = value;
                if (value > max2) max2 = value;

                converter.clear();
            }

            JPanel mainContainer = new JPanel();
            JPanel container1 = new JPanel();
            JPanel container2 = new JPanel();
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new FlowLayout());

            JPanel channel1Panel = new JPanel();
            channel1Panel.setBorder(BorderFactory.createTitledBorder("Channel 1"));
            channel1Panel.setPreferredSize(new Dimension(width, height));
            JPanel channel2Panel = new JPanel();
            channel2Panel.setBorder(BorderFactory.createTitledBorder("Channel 2"));
            channel2Panel.setPreferredSize(new Dimension(width, height));
            channel1Panel.add(linesDisplay1);
            channel2Panel.add(linesDisplay2);
            
            JLabel infoLabel = new JLabel("Sample rate: "+ String.valueOf(freq) + "        Total samples: " + String.valueOf(numFrames));
            
            container1.setLayout(new GridLayout(2,1));
            container1.add(channel1Panel);
            container1.add(channel2Panel);

            container2.setLayout(new FlowLayout());
            container2.add(infoLabel);

            JButton compress = new JButton("Compress");
            compress.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    part1_2(file, audioBytes, frame, numFrames, (int)bytesPerFrame, (int)bitsPerSample);
                }

            });

            JButton home= new JButton("SELECT ANOTHER FILE");
            home.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    prevFrame.setVisible(true);
                }

            });
            buttonsPanel.add(home);
            buttonsPanel.add(compress);

            mainContainer.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.HORIZONTAL;
            c.weighty = 0.1;
            c.gridx = 0;
            c.gridy = 1;
            mainContainer.add(container1);
            mainContainer.add(container2, c);
            c.gridy = 2;
            mainContainer.add(buttonsPanel, c);

            frame.add(mainContainer);
            frame.pack();
            frame.setVisible(true);

        } catch (Exception e) {
            System.err.println(e);
            prevFrame.setVisible(true);
        }
    } // part1


    public static void part1_2(File file, byte[] inputByteStream, JFrame prevFrame, long numSamples, int bytesPerFrame, int bitsPerSample) {
        try {
            // prevFrame.setVisible(false);
            int[] inputStream = new int[inputByteStream.length];
            // No loss of precision here
            for (int i = 0; i < inputByteStream.length; i++) {
                inputStream[i] = (int)inputByteStream[i];
            }
    
            // Audio Compression (LZW)
            ArrayList<Integer> compressedStream = LZW_Encode(inputStream, -128, 128);
            // for (int i = 0; i < compressedStream.size(); i++) {
            //     System.out.println(compressedStream.get(i));
            // }
    
            // Put codes into a string
            String compressedStream_str = "";
            for (int i = 0; i < compressedStream.size(); i++) {
                if (i == compressedStream.size()-1) compressedStream_str += compressedStream.get(i).toString() + "\n";
                else compressedStream_str += compressedStream.get(i).toString() + ",";
            }
    
            // Write to csv file
            File toWrite = new File(file.getName() + "_compressed.csv");
            if (toWrite.createNewFile()) {
                System.out.println("File created!");
            } else {
                System.out.println("File already exists");
            }
    
            FileWriter csvWriter = new FileWriter(toWrite.getName(), false);
            csvWriter.append(compressedStream_str);
            csvWriter.flush();
            csvWriter.close();
    
            // Audio Decompression
            // Read CSV file containing only the data stream
            BufferedReader csvReader = new BufferedReader(new FileReader(file.getName() + "_compressed.csv"));
            compressedStream_str = csvReader.readLine();
            csvReader.close();

            String[] compressedStream_split = compressedStream_str.split(",");
            compressedStream = new ArrayList<Integer>();
            for (int i = 0; i < compressedStream_split.length; i++) {
                compressedStream.add(Integer.parseInt(compressedStream_split[i]));
            }
           
            // Decode
            ArrayList<Integer> decompressedStream = LZW_Decode(compressedStream, -128, 128);
            // for (int i = 100000; i < 100100; i++) {
            //     System.out.println(inputStream[i]);
            // }
            // System.out.println(" ");
            // for (int i = 100000; i < 100100; i++) {
            //     System.out.println(decompressedStream.get(i));
            // }
            ArrayList<Byte> decompressedByteStream = new ArrayList<Byte>();
            for (int i = 0; i < decompressedStream.size(); i++) {
                decompressedByteStream.add(Byte.valueOf(decompressedStream.get(i).byteValue()));
            }
    
            double compressionRatio = inputByteStream.length / (double)(compressedStream.size());
            System.out.printf("COMPRESSION RATIO: %.2f", compressionRatio);
    
            // Draw waveform
            // Setup window
            int width = 1000;
            int height = 150;
    
            JFrame frame = new JFrame("Decompressed Waveforms");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 700);
            frame.setBackground(Color.black);
    
            LinesComponent linesDisplay1 = new LinesComponent();
            linesDisplay1.setPreferredSize(new Dimension(width, height));
            LinesComponent linesDisplay2 = new LinesComponent();
            linesDisplay2.setPreferredSize(new Dimension(width, height));
    
            // Read blockSize number of samples in each channel
            // Draw lines according to local mins and maxes 
            int blockSize = Math.round((int)numSamples / width);             // Try to do resizable window?
            int lineMaxLength = Math.round(height / 2);
            int channel1_xAxis = lineMaxLength;
            int channel2_xAxis = lineMaxLength;
            int x = 0;
    
            ByteBuffer converter = ByteBuffer.allocate(2);
            converter.order(ByteOrder.LITTLE_ENDIAN);
    
            byte zeroByte = 0;
            short value = 0;
            short min1 = 0;
            short max1 = 0;
            short min2 = 0;
            short max2 = 0;
            int globalAbsMaxMag = 32767;     // Max value of SHORT 
            int byteIter = 0;
            int lineLength = 0;
    
            for (int i = 0; i < numSamples; i++) {
                byteIter = i * (int)bytesPerFrame;
    
                if (i % blockSize == 0 || i == decompressedByteStream.size()-1) {
                    // Add new bar
                    // Will do 2 lines per bar, both coming from the channel's respective x-axes
                    
                    lineLength = Math.abs((int)(lineMaxLength * ((float)(min1) / globalAbsMaxMag)));
                    linesDisplay1.insertLine(x, channel1_xAxis, x, channel1_xAxis+lineLength);
                    lineLength = Math.abs((int)(lineMaxLength * ((float)(max1) / globalAbsMaxMag)));
                    linesDisplay1.insertLine(x, channel1_xAxis, x, channel1_xAxis-lineLength);
    
                    lineLength = Math.abs((int)(lineMaxLength * ((float)(min2) / globalAbsMaxMag)));
                    linesDisplay2.insertLine(x, channel2_xAxis, x, channel2_xAxis+lineLength);
                    lineLength = Math.abs((int)(lineMaxLength * ((float)(max2) / globalAbsMaxMag)));
                    linesDisplay2.insertLine(x, channel2_xAxis, x, channel2_xAxis-lineLength);
                    
                    // Reset min and max
                    min1 = 0;
                    max1 = 0;
                    min2 = 0;
                    max2 = 0;
                    x++;
                }
    
                // First channel
                if (bitsPerSample == 8) {
                    converter.put(zeroByte);
                    converter.put(decompressedByteStream.get(byteIter));
                } else if (bitsPerSample == 16) {
                    converter.put(decompressedByteStream.get(byteIter));              // Read 2 bytes as a short trick
                    converter.put(decompressedByteStream.get(byteIter+1));
                }
    
                value = converter.getShort(0);
                if (value < min1) min1 = value;
                if (value > max1) max1 = value;
                
                converter.clear();
    
                // Second channel
                if (bitsPerSample == 8) {
                    converter.put(zeroByte);
                    converter.put(decompressedByteStream.get(byteIter+1));
                } else if (bitsPerSample == 16) {
                    converter.put(decompressedByteStream.get(byteIter+2));
                    converter.put(decompressedByteStream.get(byteIter+3));
                }
    
                // Add this value to my short input stream (For compression)
                value = converter.getShort(0);
                if (value < min2) min2 = value;
                if (value > max2) max2 = value;
    
                converter.clear();
            }
    
            JPanel mainContainer = new JPanel();
            JPanel container1 = new JPanel();
            JPanel container2 = new JPanel();
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new FlowLayout());

            JButton back = new JButton("Back");
            back.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    prevFrame.setVisible(true);
                }

            });
            buttonsPanel.add(back); 
    
            JPanel channel1Panel = new JPanel();
            channel1Panel.setBorder(BorderFactory.createTitledBorder("Channel 1"));
            channel1Panel.setPreferredSize(new Dimension(width, height));
            JPanel channel2Panel = new JPanel();
            channel2Panel.setBorder(BorderFactory.createTitledBorder("Channel 2"));
            channel2Panel.setPreferredSize(new Dimension(width, height));
            channel1Panel.add(linesDisplay1);
            channel2Panel.add(linesDisplay2);
    
            JLabel infoLabel = new JLabel("Compression rate: "+ String.valueOf(compressionRatio));
                
            container1.setLayout(new GridLayout(2,1));
            container1.add(channel1Panel);
            container1.add(channel2Panel);
    
            container2.setLayout(new FlowLayout());
            container2.add(infoLabel);
    
            mainContainer.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
    
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weighty = 0.1;
            c.gridx = 0;
            c.gridy = 1;
            mainContainer.add(container1);
            mainContainer.add(container2, c);
            c.gridy = 2;
            mainContainer.add(buttonsPanel, c);
    
            frame.add(mainContainer);
            frame.pack();
            frame.setVisible(true);

        } catch (Exception e) {
            System.err.println(e);
            prevFrame.setVisible(true);
        }
    } // part1_2


    public static void part2(File file, JFrame prevFrame) {
        try {
            prevFrame.setVisible(false);

            BufferedImage buffered = ImageIO.read(file);

            JFrame frame = new JFrame(file.getName() + " - RGB Histograms");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Histograms
            // Read image, store in matrix
            DataBuffer imData = buffered.getData().getDataBuffer();
            int numRows = buffered.getData().getHeight();
            int numCols = buffered.getData().getWidth();
            int imSize = buffered.getData().getHeight() * buffered.getData().getWidth();
            
            // Histogram arrays
            int[] R = new int[256];     // Apparently these are initialized with zeroes?
            int[] G = new int[256];
            int[] B = new int[256];
            int[][] rImage = new int[numRows][numCols];     // Image maps of each color channel (for drawing)
            int[][] gImage = new int[numRows][numCols]; 
            int[][] bImage = new int[numRows][numCols];
            int pixelIndex = 0;
            int iter = 0;
            
            // Iterate through each pixel, reading each channel
            for (int i = 0; i < numRows ; i++) {
                for (int j = 0; j < numCols; j++) {
                    pixelIndex = (i*numCols) + j;
                    iter = pixelIndex * 3;

                    R[imData.getElem(iter)]++;
                    G[imData.getElem(iter+1)]++;
                    B[imData.getElem(iter+2)]++;

                    rImage[i][j] = imData.getElem(iter);
                    gImage[i][j] = imData.getElem(iter+1);
                    bImage[i][j] = imData.getElem(iter+2);
                }
            }

            // Drawing Image
            BufferedImage bufferedImThatIDidMyself = drawImageRGB(numCols, numRows, rImage, gImage, bImage);
            ImageIcon image = new ImageIcon(bufferedImThatIDidMyself);

            // Drawing Histograms
            int xDim = 257;     // +1 to draw axes
            int yDim = 257;     // yDim is variable but xDim not...
            int x = 1;
            int lineLength = 0;
            
            LinesComponent rHist = new LinesComponent();
            LinesComponent gHist = new LinesComponent();
            LinesComponent bHist = new LinesComponent();
            rHist.setPreferredSize(new Dimension(xDim, yDim));
            gHist.setPreferredSize(new Dimension(xDim, yDim));
            bHist.setPreferredSize(new Dimension(xDim, yDim));
            
            
            for (int i = 0; i < R.length; i++) {
                lineLength = Math.abs((int)((yDim - 1) * ((float)R[i] / (imSize / 10))));
                rHist.insertLine(x, yDim-1, x, (yDim-1) - lineLength, Color.red);
                
                lineLength = Math.abs((int)((yDim - 1) * ((float)G[i] / (imSize / 10))));
                gHist.insertLine(x, yDim-1, x, (yDim-1) - lineLength, Color.green);
                
                lineLength = Math.abs((int)((yDim - 1) * ((float)B[i] / (imSize / 10))));
                bHist.insertLine(x, yDim-1, x, (yDim-1) - lineLength, Color.blue);
                
                x++;
            }
            
            // Axes
            rHist.insertLine(0, 0, 0, yDim-1, Color.black);
            rHist.insertLine(xDim-1, yDim-1, 0, yDim-1, Color.black);
            gHist.insertLine(0, 0, 0, yDim-1, Color.black);
            gHist.insertLine(xDim-1, yDim-1, 0, yDim-1, Color.black);
            bHist.insertLine(0, 0, 0, yDim-1, Color.black);
            bHist.insertLine(xDim-1, yDim-1, 0, yDim-1, Color.black);
            
            JButton dither = new JButton("Dither");
            dither.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    part2a(file, frame);
                }
                
            });

            JButton compress = new JButton("Compress");
            compress.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectQuality(file, frame);
                }
                
            });

            JButton home= new JButton("SELECT ANOTHER FILE");
            home.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    prevFrame.setVisible(true);
                }

            });
            
            
            // Display Image
            JLabel label = new JLabel();
            label.setIcon(image);
            
            JPanel mainContainer = new JPanel();
            mainContainer.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            
            JPanel imContainer = new JPanel();
            JPanel histContainer = new JPanel();
            histContainer.setLayout(new FlowLayout());
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new FlowLayout());
            
            imContainer.add(label);
            histContainer.add(rHist);
            histContainer.add(gHist);
            histContainer.add(bHist);
            buttonsPanel.add(dither);
            buttonsPanel.add(compress);
            buttonsPanel.add(home);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.weighty = 0.1;
            c.gridx = 0;
            c.gridy = 2;
            
            mainContainer.add(imContainer);
            mainContainer.add(histContainer);
            mainContainer.add(buttonsPanel, c);

            frame.add(mainContainer);
            frame.pack();
            frame.setVisible(true);
            
        } catch (Exception e) {
            System.err.println(e);
            prevFrame.setVisible(true);
        }
    } // part2


    public static void part2a(File file, JFrame prevFrame) {
        try {
            prevFrame.setVisible(false);

            // Dithering
            BufferedImage buffered = ImageIO.read(file);
            
            int numRows = buffered.getData().getHeight();
            int numCols = buffered.getData().getWidth();
            DataBuffer imData = buffered.getData().getDataBuffer();
            
            // Get image data from each channel, store in matrices per channel
            int[][] rImage = new int[numRows][numCols];
            int[][] gImage = new int[numRows][numCols]; 
            int[][] bImage = new int[numRows][numCols];
            int pixelIndex = 0;
            int iter = 0;  
            
            for (int row = 0; row < numRows; row++) {
                for (int col = 0; col < numCols; col++) {
                    pixelIndex = (row*numCols) + col;
                    iter = pixelIndex * 3;              // 3 channels, RGB
                    
                    rImage[row][col] = imData.getElem(iter);
                    gImage[row][col] = imData.getElem(iter+1);
                    bImage[row][col] = imData.getElem(iter+2);
                }
            }
            
            BufferedImage bufferedImThatIDidMyself = drawImageRGB(numCols, numRows, rImage, gImage, bImage);
            ImageIcon image = new ImageIcon(bufferedImThatIDidMyself);
            // Possible dither matrices
            // int[][] ditherMat = {               // 2x2
            //         {0, 2},
            //         {3, 1}
            //     };
                
            // int[][] ditherMat = {               // 3x3 (https://codeantenna.com/a/aUqsNPEL6N)
            //     {0, 7, 3},
            //     {6, 5, 2},
            //     {4, 1, 8}
            // };

            // int[][] ditherMat = {               // 4x4
            //     {0, 8, 2, 10},
            //     {12, 4, 14, 6},
            //     {3, 11, 1, 9},
            //     {15, 7, 13, 5}
            // };

            int[][] ditherMat = {               // 8x8 (https://en.wikipedia.org/wiki/Ordered_dithering)
                {0, 32, 8, 40, 2, 34, 10, 42},
                {48, 16, 56, 24, 50, 18, 58, 26},
                {12, 44, 4, 36, 14, 46, 6, 38},
                {60, 28, 52, 20, 62, 30, 54, 22},
                {3, 35, 11, 43, 1, 33, 9, 41},
                {51, 19, 59, 27, 49, 17, 57, 25},
                {15, 47, 7, 39, 13, 45, 5, 37},
                {63, 31, 55, 23, 61, 29, 53, 21}
            };

            int[][] rDithered = orderedDither(rImage, ditherMat, numCols, numRows);
            int[][] gDithered = orderedDither(gImage, ditherMat, numCols, numRows);
            int[][] bDithered = orderedDither(bImage, ditherMat, numCols, numRows);

            // We have our 3 dithered channels, now make an image
            BufferedImage ditherBuffered = drawImageRGB(numCols, numRows, rDithered, gDithered, bDithered);
            ImageIcon imDithered = new ImageIcon(ditherBuffered);


            JFrame frame = new JFrame(file.getName() + " - Ordered Dither");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Display Images
            JPanel mainContainer = new JPanel();
            mainContainer.setLayout(new GridBagLayout());
            
            JPanel imagesPanel = new JPanel();
            imagesPanel.setLayout(new FlowLayout());
            imagesPanel.setBackground(Color.black);
            JLabel label = new JLabel();
            JLabel label2 = new JLabel();
            label.setIcon(image);
            label2.setIcon(imDithered);
            imagesPanel.add(label);
            imagesPanel.add(label2);

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new FlowLayout());
            JButton back = new JButton("Back");
            back.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    prevFrame.setVisible(true);
                }

            });
            buttonsPanel.add(back);

            mainContainer.add(imagesPanel);
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weighty = 0.1;
            c.gridx = 0;
            c.gridy = 1;
            mainContainer.add(buttonsPanel, c);

            frame.add(mainContainer);
            frame.pack();
            frame.setVisible(true);

        } catch (Exception e) {
            System.err.println(e);
            prevFrame.setVisible(true);
        }
    } // part2_2


    public static void part2b(File file, JFrame prevFrame, int qf) {
        try {
            BufferedImage buffered = ImageIO.read(file);
            DataBuffer imData = buffered.getData().getDataBuffer();
            int width = buffered.getWidth();
            int height = buffered.getHeight();
            
            // Separate into the RGB channels
            int[][] rChannel = new int[height][width];
            int[][] gChannel = new int[height][width];
            int[][] bChannel = new int[height][width];
            int pxIndex = 0;
            int iter = 0;
            
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    pxIndex = (row*width) + col;
                    iter = pxIndex*3;
                    
                    rChannel[row][col] = imData.getElem(iter);
                    gChannel[row][col] = imData.getElem(iter+1);
                    bChannel[row][col] = imData.getElem(iter+2);
                }
            }
            // Save original image for display
            BufferedImage bufferedImThatIDidMyself = drawImageRGB(width, height, rChannel, gChannel, bChannel);
            ImageIcon image = new ImageIcon(bufferedImThatIDidMyself);
            
            // Image Compression
            // RGB to YUV 4:2:0 first
            
            // Dimensions can be odd -.-
            int uvWidth;
            int uvHeight;
            if (width % 2 == 0) uvWidth = width/2;
            else uvWidth = (width/2) + 1;
            if (height % 2 == 0) uvHeight = height/2;
            else uvHeight = (width/2) + 1;
            
            int[][] yChannel = new int[height][width];
            int[][] uChannel = new int[height][width];
            int[][] vChannel = new int[height][width];
            int[][] uChannel_downSampled = new int[uvHeight][uvWidth];
            int[][] vChannel_downSampled = new int[uvHeight][uvWidth];
            
            // On every even row and column, calculate YUV
            // Every other time, calculate only Y
            // double[][] RGB2YUV = {                           // From slides
            //         {0.299, 0.587, 0.114},
            //         {-0.299, -0.587, 0.886},
            //         {0.701, -0.587, -0.114}
            //     };
            // double[][] RGB2Y = {{0.299, 0.587, 0.114}};
            double[][] RGB2YUV = {                              // From textbook
                {0.299, 0.587, 0.114},
                {-0.147133, -0.28886, 0.436},
                {0.615, -0.51499, -0.10001}
            };
            // double[][] RGB2Y = {{0.299, 0.587, 0.114}};
            double[][] RGBvec = new double[3][1];
            double[][] YUV;
            
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    RGBvec[0][0] = rChannel[row][col];
                    RGBvec[1][0] = bChannel[row][col];
                    RGBvec[2][0] = gChannel[row][col];
                    
                    YUV = matMultiply(RGB2YUV, RGBvec);
                    
                    yChannel[row][col] = (int)Math.round(YUV[0][0]);
                    uChannel[row][col] = (int)Math.round(YUV[1][0]);
                    vChannel[row][col] = (int)Math.round(YUV[2][0]);
                }
            }
            
            // For U and V, take average in a 2x2 block and used that
            int rowIdx = 0;
            int colIdx = 0;
            for (int row = 0; row < height; row+=2) {
                for (int col = 0; col < width; col+=2) {
                    // System.out.println(row + " " + col);
                    if (row >= height || col >= width) break;
                    // Edge cases
                    rowIdx = row / 2;
                    colIdx = col / 2;  

                    if (row == height-1 && col == width-1) {
                        uChannel_downSampled[rowIdx][colIdx] = (int)Math.round(uChannel[row][col]);
                        vChannel_downSampled[rowIdx][colIdx] = (int)Math.round(vChannel[row][col]);
                    } else if (row == height-1) {
                        uChannel_downSampled[rowIdx][colIdx] = (int)Math.round((uChannel[row][col] + uChannel[row][col+1]) / 2.0);
                        vChannel_downSampled[rowIdx][colIdx] = (int)Math.round((vChannel[row][col] + vChannel[row][col+1]) / 2.0);
                    } else if (col == width-1) {
                        uChannel_downSampled[rowIdx][colIdx] = (int)Math.round((uChannel[row][col] + uChannel[row+1][col]) / 2.0);
                        vChannel_downSampled[rowIdx][colIdx] = (int)Math.round((vChannel[row][col] + vChannel[row+1][col]) / 2.0);

                    } else {
                        uChannel_downSampled[rowIdx][colIdx] = (int)Math.round((uChannel[row][col] + uChannel[row+1][col] + uChannel[row][col+1] + uChannel[row+1][col+1]) / 4.0);
                        vChannel_downSampled[rowIdx][colIdx] = (int)Math.round((vChannel[row][col] + vChannel[row+1][col] + vChannel[row][col+1] + vChannel[row+1][col+1]) / 4.0);
                    }
                }
            }
            
            System.out.println("In Part 2 B...");
            int N = 8;          // Block size
            double[][] DCTmat = GenerateDCTTransformMatrix(N);
            // double[][] QuantTable_slides = {
            //     {1, 1, 2, 4, 8, 16, 32, 64},
            //     {1, 1, 2, 4, 8, 16, 32, 64},
            //     {2, 2, 2, 4, 8, 16, 32, 64},
            //     {4, 4, 4, 4, 8, 16, 32, 64},
            //     {8, 8, 8, 8, 8, 16, 32, 64},
            //     {16, 16, 16, 16, 16, 16, 32, 64},
            //     {32, 32, 32, 32, 32, 32, 32, 64},
            //     {64, 64, 64, 64, 64, 64, 64, 64}
            // };
            double[][] QuantTable_Y = {
                {16, 11, 10, 16, 24, 40, 51, 61},
                {12, 12, 14, 19, 26, 58, 60, 55},
                {14, 13, 16, 24, 40, 57, 69, 56},
                {14, 17, 22, 29, 51, 87, 80, 62},
                {18, 22, 37, 56, 68, 109, 103, 77},
                {24, 35, 55, 64, 81, 104, 113, 92},
                {49, 64, 78, 87, 103, 121, 120, 101},
                {72, 92, 95, 98, 112, 100, 103, 99}
            };
            double[][] QuantTable_Chrom = {
                {17, 18, 24, 47, 99, 99, 99, 99},
                {18, 21, 26, 66, 99, 99, 99, 99},
                {24, 26, 56, 99, 99, 99, 99, 99},
                {47, 66, 99, 99, 99, 99, 99, 99},
                {99, 99, 99, 99, 99, 99, 99, 99},
                {99, 99, 99, 99, 99, 99, 99, 99},
                {99, 99, 99, 99, 99, 99, 99, 99},
                {99, 99, 99, 99, 99, 99, 99, 99}
            };
            
            String yChannel_Encoded = CompressImageChannel(N, yChannel, DCTmat, ScaleQuantizationTable(qf, QuantTable_Y));
            String uChannel_Encoded = CompressImageChannel(N, uChannel_downSampled, DCTmat, ScaleQuantizationTable(qf, QuantTable_Chrom));
            String vChannel_Encoded = CompressImageChannel(N, vChannel_downSampled, DCTmat, ScaleQuantizationTable(qf, QuantTable_Chrom));
    
            // Write to file
            // Each channel is a single line, comma-separated
            File toWrite = new File(file.getName() + "_compressed.csv");
            if (toWrite.createNewFile()) {
                System.out.println("File created!");
            } else {
                System.out.println("File already exists");
            }

            
            // Overwrite anything if it exists already
            FileWriter csvWriter = new FileWriter(toWrite.getName(), false);
            // Have a few headers: N, qf, image dimensions
            csvWriter.append(String.valueOf(N) + "," + String.valueOf(qf) + "," + String.valueOf(width) + "," + String.valueOf(height) + "\n");
            csvWriter.append(yChannel_Encoded);
            csvWriter.append(uChannel_Encoded);
            csvWriter.append(vChannel_Encoded);

            csvWriter.flush();
            csvWriter.close();


            // Image Decompression - Basically work backwards, pretend that this is isolated from compression
            // Read CSV file, split into each channel
            BufferedReader csvReader = new BufferedReader(new FileReader(file.getName() + "_compressed.csv"));
            String[] headers = csvReader.readLine().split(",");
            N = Integer.parseInt(headers[0]);
            qf = Integer.parseInt(headers[1]);
            width = Integer.parseInt(headers[2]);
            height = Integer.parseInt(headers[3]);
            yChannel_Encoded = csvReader.readLine();
            uChannel_Encoded = csvReader.readLine();
            vChannel_Encoded = csvReader.readLine();

            csvReader.close();

            int[][] yChannel_Decoded = DecompressImageChannel(N, yChannel_Encoded, width, height, DCTmat, ScaleQuantizationTable(qf, QuantTable_Y));
            int[][] uChannel_Decoded = DecompressImageChannel(N, uChannel_Encoded, uvWidth, uvHeight, DCTmat, ScaleQuantizationTable(qf, QuantTable_Chrom));
            int[][] vChannel_Decoded = DecompressImageChannel(N, vChannel_Encoded, uvWidth, uvHeight, DCTmat, ScaleQuantizationTable(qf, QuantTable_Chrom));

            // YUV 4:2:0 to RGB
            // double[][] YUV2RGB = {                     // Inverse of the matrix from slides
            //     {1, 0, 1}, 
            //     {1, -0.19420784, -0.50936968}, 
            //     {1, 1, 0}
            // };
            double[][] YUV2RGB = {                     // Matrix from textbook
                {1, 0, 1}, 
                {1, -0.39465, -0.58060}, 
                {1, 2.03211, 0}
            };
            double[][] YUVvec = new double[3][1];
            double[][] RGB;

            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    // New Y value on every iteration
                    // Read new U, V on every even row and column
                    YUVvec[0][0] = yChannel_Decoded[row][col];
                    // Integer division always takes the floor so we're ok
                    YUVvec[1][0] = uChannel_Decoded[row/2][col/2];
                    YUVvec[2][0] = vChannel_Decoded[row/2][col/2];
                   
                    RGB = matMultiply(YUV2RGB, YUVvec);
                    
                    rChannel[row][col] = (int)Math.round(RGB[0][0]);
                    bChannel[row][col] = (int)Math.round(RGB[1][0]);  
                    gChannel[row][col] = (int)Math.round(RGB[2][0]);
                    
                    // Overflow / Underflow, clamp it
                    if (rChannel[row][col] > 255) rChannel[row][col] = 255;
                    if (gChannel[row][col] > 255) gChannel[row][col] = 255;
                    if (bChannel[row][col] > 255) bChannel[row][col] = 255;

                    if (rChannel[row][col] < 0) rChannel[row][col] = 0;
                    if (gChannel[row][col] < 0) gChannel[row][col] = 0;
                    if (bChannel[row][col] < 0) bChannel[row][col] = 0;
                }
            }

            // Display decompressed image
            BufferedImage decompressBuffered = drawImageRGB(width, height, rChannel, gChannel, bChannel);
            ImageIcon imDecompressed = new ImageIcon(decompressBuffered);

            JFrame frame = new JFrame(file.getName() + " - After Compression");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel mainContainer = new JPanel();

            JPanel imagesPanel = new JPanel();
            JPanel buttonsPanel = new JPanel();
            mainContainer.setLayout(new GridBagLayout());
            imagesPanel.setLayout(new FlowLayout());
            imagesPanel.setBackground(Color.black);
            buttonsPanel.setLayout(new FlowLayout());

            JLabel im1 = new JLabel();
            JLabel im2 = new JLabel();
            im1.setIcon(image);
            im2.setIcon(imDecompressed);
            imagesPanel.add(im1);
            imagesPanel.add(im2);

            JButton back = new JButton("Back");
            back.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    prevFrame.setVisible(true);
                }

            });
            buttonsPanel.add(back);

            mainContainer.add(imagesPanel);
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weighty = 0.1;
            c.gridx = 0;
            c.gridy = 1;
            mainContainer.add(buttonsPanel, c);

            frame.add(mainContainer);
            frame.pack();
            frame.setVisible(true);

        } catch (Exception e) {
            System.err.println(e);
            prevFrame.setVisible(true);
        }
    } // part2b


    public static void selectQuality(File file, JFrame prevFrame) {
        try {
            prevFrame.setVisible(false);

            JFrame frame = new JFrame(file.getName() + " - Select Compression Quality");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel mainContainer = new JPanel();
            mainContainer.setLayout(new FlowLayout());

            JPanel buttonsPanel1 = new JPanel();
            buttonsPanel1.setLayout(new FlowLayout());
            JButton terrible = new JButton("Terrible");
            terrible.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    part2b(file, prevFrame, 1);
                }

            });
            JButton veryLow = new JButton("Very Low");
            veryLow.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    part2b(file, prevFrame, 10);
                }

            });
            JButton low = new JButton("Low");
            low.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    part2b(file, prevFrame, 30);
                }

            });
            JButton med = new JButton("Medium");
            med.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    part2b(file, prevFrame, 50);
                }

            });
            JButton high = new JButton("High");
            high.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    part2b(file, prevFrame, 70);
                }

            });
            JButton veryHigh = new JButton("Very High");
            veryHigh.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    part2b(file, prevFrame, 90);
                }

            });
            JButton minLoss = new JButton("Minimum Loss");
            minLoss.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    part2b(file, prevFrame, 100);
                }

            });
            buttonsPanel1.add(terrible);
            buttonsPanel1.add(veryLow);
            buttonsPanel1.add(low);
            buttonsPanel1.add(med);
            buttonsPanel1.add(high);
            buttonsPanel1.add(veryHigh);
            buttonsPanel1.add(minLoss);

            JPanel buttonsPanel2 = new JPanel();
            buttonsPanel2.setLayout(new FlowLayout());
            JButton home= new JButton("CANCEL");
            home.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    prevFrame.setVisible(true);
                }

            });
            buttonsPanel2.add(home);

            mainContainer.add(buttonsPanel1);
            mainContainer.add(buttonsPanel2);

            frame.add(mainContainer);
            frame.pack();
            frame.setVisible(true);

        } catch (Exception e) {
            System.err.println(e);
            prevFrame.setVisible(true);
        }

    } // selectQuality


    /* PROCESSES*/
    public static ArrayList<Integer> LZW_Encode(int[] inputStream, int min_alphabet, int max_alphabet) {
        int max_dict_size = 10000;
        ArrayList<String> dictionary = new ArrayList<String>();
        // Initial dictionary should be the alphabet [min byte, max byte]
        for (int i = min_alphabet; i < max_alphabet; i++) {
            dictionary.add(String.valueOf(i) + ",");
        }
        
        String seen = "";
        String nextChar = "";
        ArrayList<Integer> outputStream = new ArrayList<Integer>();

        for (int i = 0; i < inputStream.length; i++) {
            if (i % 10000 == 0) System.out.println(i + "/" + inputStream.length);
            
            nextChar = String.valueOf(inputStream[i] + ",");
            
            if (dictionary.indexOf(seen + nextChar) != -1) {
                seen += nextChar;
            } else {
                outputStream.add(Integer.valueOf(dictionary.indexOf(seen)));                                       // Output code for s
                // If we haven't yet reached max size, keep building the dictionary
                if (dictionary.size() < max_dict_size) dictionary.add(seen + nextChar);     // New entry into dictionary
                seen = nextChar;                                                            // Reset s
            }
        
        }
        outputStream.add(Integer.valueOf(dictionary.indexOf(seen)));                        // For any unnaccounted characters

        // for (int i = 255; i < 300; i++) {
        //     System.out.println("DICTIONARY *** key:" + i + "\tstring:" + dictionary.get(i));
        // }

        return outputStream;
    }


    public static ArrayList<Integer> LZW_Decode(ArrayList<Integer> compressedStream, int min_alphabet, int max_alphabet) {
        // Build the same dictionary as encoder
        int max_dict_size = 10000;
        ArrayList<String> dictionary = new ArrayList<String>();
        for (int i = min_alphabet; i < max_alphabet; i++) {
            dictionary.add(String.valueOf(i) + ",");
        }

        String seen = "";
        String[] parsed_seen;
        String dict_entry = "";
        String[] parsed_entry;
        Integer nextCode = 0;
        ArrayList<Integer> outputStream = new ArrayList<Integer>();

        System.out.println("\nIN DECODE");
        for (int i = 0; i < compressedStream.size(); i++) {
            if (i % 10000 == 0) {
                System.out.println(i + "/" + compressedStream.size());
            }

            // Read next code
            parsed_seen = seen.split(",");
            nextCode = compressedStream.get(i);

            if (dictionary.size() < max_dict_size) {
                // Output dictionary entry
                if (nextCode >= dictionary.size()) {
                    // Corner case (in textbook)
                    dict_entry = seen + parsed_seen[0] + ",";
                } else {
                    dict_entry = dictionary.get(nextCode);
                }
                parsed_entry = dict_entry.split(",");
    
                for (int k = 0; k < parsed_entry.length; k++) {
                    outputStream.add(Integer.valueOf(parsed_entry[k]));
                }
    
                if (seen != "") {
                    // Add s + entry[0] to dictionary
                    dictionary.add(seen + parsed_entry[0] + ",");
                }
    
                seen = dict_entry;

            } else {
                // It's just a table lookup now
                dict_entry = dictionary.get(nextCode);
                parsed_entry = dict_entry.split(",");

                for (int k = 0; k < parsed_entry.length; k++) {
                    outputStream.add(Integer.valueOf(parsed_entry[k]));
                }
            }
        }

        // for (int i = dictionary.size()-100; i < dictionary.size(); i++) {
        //     System.out.println("DECOMP DICTIONARY *** key:" + i + "\tstring: " + dictionary.get(i));
        // }

        return outputStream;
    }


    // Returns a string, each of which is the codes of an image channel
    public static String CompressImageChannel(int N, int[][] data, double[][] DCTmat, double[][] quantTable) {
        System.out.println("Compressing...");
        double[][] block = new double[N][N];
        int[][] block_Quantized = new int[N][N];
        double[][] DCTmat_transpose = matTranspose(DCTmat);

        int prev_DC = 0;
        int curr_DC = 0;
        int direction = 0;      // zigzag: 0 = up-right, 1 = down-left
        int iter_row = 0;
        int iter_col = 0;
        int num_zeros = 0;
        
        ArrayList<Integer> toEncode = new ArrayList<Integer>();
        
        // Compress each NxN block
        int block_start_idx_row = 0;
        int block_start_idx_col = 0;
        for (int row = 0; row < data.length; row += N) {
            for (int col = 0; col < data[0].length; col += N) {
                // System.out.println("Compressing... block " + row + " " + col);
                // Sequential Mode
                // Edge cases
                if (row + N >= data.length) block_start_idx_row = data.length - N - 1;
                else block_start_idx_row = row;
                if (col + N >= data[0].length) block_start_idx_col = data[0].length - N - 1;
                else block_start_idx_col = col;
                
                // Step 1: Block prep
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        block[i][j] = data[block_start_idx_row + i][block_start_idx_col + j];
                    }
                }

                // Step 2: DCT on each 8x8 block for each channel (rows first)
                block = matMultiply(DCTmat, matMultiply(block, DCTmat_transpose));
                
                if (block_start_idx_row == 0 && block_start_idx_col == 0) {
                    for (int i = 0; i < N; i++) {
                        System.out.println(block[i][0] + "\t" + block[i][1] + "\t" + block[i][2] + "\t" + block[i][3] + "\t" + block[i][4] + "\t" + block[i][5] + "\t" + block[i][6] + "\t" + block[i][7]);
                    }
                    System.out.println();
                }

                // Step 3: Quantization of block
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        block_Quantized[i][j] = (int)Math.round(block[i][j] / quantTable[i][j]);
                    }
                }

                if (block_start_idx_row == 0 && block_start_idx_col == 0) {
                    for (int i = 0; i < N; i++) {
                        System.out.println(block_Quantized[i][0] + "\t" + block_Quantized[i][1] + "\t" + block_Quantized[i][2] + "\t" + block_Quantized[i][3] + "\t" + block_Quantized[i][4] + "\t" + block_Quantized[i][5] + "\t" + block_Quantized[i][6] + "\t" + block_Quantized[i][7]);
                    }
                    System.out.println();
                }

                // Step 4: Encoding
                direction = 0;          // 0 = up-right, 1 = down-left
                iter_row = 0;
                iter_col = 0;
                num_zeros = 0;


                // Zigzag Scan
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        if (iter_row == 0 && iter_col == 0) {
                            // Differential Pulse Code Modulation on DC Component
                            // Encode the difference from previous block's DC Component
                            curr_DC = block_Quantized[0][0];
                            toEncode.add(curr_DC - prev_DC);
                            
                            // if (block_start_idx_row == 0 && block_start_idx_col == 0) {
                            //     System.out.println(prev_DC + " " + curr_DC + " " +toEncode.get(toEncode.size()-1));
                            //     System.out.println();
                            // } 

                            prev_DC = curr_DC;
                        } else {
                            // Run Length Encoding on AC Components
                            if (block_Quantized[iter_row][iter_col] == 0) {
                                // While we are still reading zeros, increment zero count
                                num_zeros++;
                            } else {
                                // If we hit a non-zero, encode number of zeros then the non-zero entry
                                toEncode.add(num_zeros);
                                toEncode.add(block_Quantized[iter_row][iter_col]);
                                num_zeros = 0;
                                // if (block_start_idx_row == 0 && block_start_idx_col == 0) {
                                //     System.out.println(toEncode.get(toEncode.size()-2));
                                //     System.out.println(toEncode.get(toEncode.size()-1));
                                //     System.out.println();
                                // } 
                            }
                            
                            // If we hit the end, encode (0, 0)
                            if (iter_row == N-1 && iter_col == N-1) {
                                toEncode.add(0);
                                toEncode.add(0);
                                // if (block_start_idx_row == 40 && block_start_idx_col == 40) {
                                //     System.out.println(toEncode.get(toEncode.size()-2));
                                //     System.out.println(toEncode.get(toEncode.size()-1));
                                // } 
                            }
                        }

                        

                        if (direction == 0) {
                            if (iter_row == 0) {                        // If we hit the top row, go right
                                iter_row = 0;
                                iter_col++;
                                direction = 1;
                            } else if (iter_col == N-1) {               // If we hit the right column, go down
                                iter_row++;
                                iter_col = N-1;
                                direction = 1;
                            } else {
                                iter_row--;
                                iter_col++;
                            }
                        } else {
                            if (iter_row == N-1) {                      // If we hit the bottom row, go right
                                iter_row = N-1;
                                iter_col++;
                                direction = 0;
                            } else if (iter_col == 0) {                 // If we hit the left column, go down
                                iter_row++;
                                iter_col = 0;
                                direction = 0;
                            } else {
                                iter_row++;
                                iter_col--;
                            }
                        }
                    }
                }
            }
        }

        // Entropy Coding (Huffman? / Arithmetic? / LZW?)
        // Convert the ArrayList of Bytes into an array of ints
        int[] forLZW = new int[toEncode.size()];
        for (int i = 0; i < toEncode.size(); i++) {
            forLZW[i] = toEncode.get(i);
        }
        ArrayList<Integer> EncodedChannel = LZW_Encode(forLZW, -2000, 2000);

        // Convert codes to comma-separated string
        String ret = "";
        for (int i = 0; i < EncodedChannel.size()-1; i++) {
            ret += String.valueOf(EncodedChannel.get(i)) + ",";
        }
        ret += String.valueOf(EncodedChannel.get(EncodedChannel.size()-1));
        // Newline to represent end of channel
        ret += "\n";

        return ret;
    }


    // Given a string of data, returns 
    public static int[][] DecompressImageChannel(int N, String encodedDataString, int imWidth, int imHeight, double[][] DCTmat, double[][] quantTable) {
        System.out.println("Decompressing...");
        String[] dataString = encodedDataString.split(",");
        ArrayList<Integer> encodedData = new ArrayList<Integer>();

        for (int i = 0; i < dataString.length; i++) {
            encodedData.add(Integer.valueOf(dataString[i]));
        }

        // Step 1: Decode the entropy coding
        ArrayList<Integer> DecodedChannel = LZW_Decode(encodedData, -2000, 2000);
        System.out.println(DecodedChannel.size());

        int[][] imChannel = new int[imHeight][imWidth];
        double[][] block = new double[N][N];
        int[][] block_Quantized = new int[N][N];
        double[][] DCTmat_transpose = matTranspose(DCTmat);

        // Build each block one at a time
        int block_start_idx_row = 0;
        int block_start_idx_col = 0;
        int decodedChannel_iterator = 0;

        int prev_DC = 0;
        int curr_DC = 0;
        int direction = 0;      // zigzag: 0 = up-right, 1 = down-left
        int iter_row = 0;
        int iter_col = 0;
        int num_zeros = 0;
        int zero_counter = 0;
        int first_non_zero = 0;

        for (int row = 0; row < imHeight; row += N) {
            for (int col = 0; col < imWidth; col += N) {
                // Edge cases
                if (row + N >= imHeight) block_start_idx_row = imHeight - N - 1;
                else block_start_idx_row = row;
                if (col + N >= imWidth) block_start_idx_col = imWidth - N - 1;
                else block_start_idx_col = col;
                
                // KINDA SUS I HOPE IT WORKS LIKE THIS
                // Step 2: Block prep
                // Read through Run Length Codes, if we read double 0, we just read a block
                direction = 0;          // 0 = up-right, 1 = down-left
                iter_row = 0;
                iter_col = 0;
                
                // Zigzag Scan
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        if (iter_row == 0 && iter_col == 0) {
                            // Differential Pulse Code Modulation on DC Component
                            curr_DC = (int)(DecodedChannel.get(decodedChannel_iterator) + prev_DC);
                            decodedChannel_iterator++;
                            block_Quantized[0][0] = curr_DC;

                            prev_DC = curr_DC;
                            // Read next value
                            num_zeros = DecodedChannel.get(decodedChannel_iterator);
                            decodedChannel_iterator++;
                        } else {
                            // Run Length Encoding on AC Components
                            if (zero_counter < num_zeros) {
                                block_Quantized[iter_row][iter_col] = 0;
                                zero_counter++;
                            } else {
                                // Read next value
                                // Don't want to iterate just yet
                                first_non_zero = DecodedChannel.get(decodedChannel_iterator);
                                
                                if (num_zeros == 0 && first_non_zero == 0) {
                                    // If the pair is (0, 0) just keep dropping zeros until the end
                                    block_Quantized[iter_row][iter_col] = 0;
                                } else {
                                    decodedChannel_iterator++;
                                    block_Quantized[iter_row][iter_col] = first_non_zero;
                                    // Read next value
                                    num_zeros = DecodedChannel.get(decodedChannel_iterator);
                                    decodedChannel_iterator++;
                                    zero_counter = 0;
                                }
                            }
                        }

                        if (direction == 0) {
                            if (iter_row == 0) {                        // If we hit the top row, go right
                                iter_row = 0;
                                iter_col++;
                                direction = 1;
                            } else if (iter_col == N-1) {               // If we hit the right column, go down
                                iter_row++;
                                iter_col = N-1;
                                direction = 1;
                            } else {
                                iter_row--;
                                iter_col++;
                            }
                        } else {
                            if (iter_row == N-1) {                      // If we hit the bottom row, go right
                                iter_row = N-1;
                                iter_col++;
                                direction = 0;
                            } else if (iter_col == 0) {                 // If we hit the left column, go down
                                iter_row++;
                                iter_col = 0;
                                direction = 0;
                            } else {
                                iter_row++;
                                iter_col--;
                            }
                        }
                    }
                }

                // Increment for the next block
                decodedChannel_iterator++;

                if (block_start_idx_row == 0 && block_start_idx_col == 0) {
                    for (int i = 0; i < N; i++) {
                        System.out.println(block_Quantized[i][0] + "\t" + block_Quantized[i][1] + "\t" + block_Quantized[i][2] + "\t" + block_Quantized[i][3] + "\t" + block_Quantized[i][4] + "\t" + block_Quantized[i][5] + "\t" + block_Quantized[i][6] + "\t" + block_Quantized[i][7]);
                    }
                    System.out.println();
                }

                // Step 3: De-Quantize each block
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        block[i][j] = block_Quantized[i][j] * quantTable[i][j];
                    }
                }

                // if (block_start_idx_row == 0 && block_start_idx_col == 0) {
                //     for (int i = 0; i < N; i++) {
                //         System.out.println(block[i][0] + "\t" + block[i][1] + "\t" + block[i][2] + "\t" + block[i][3] + "\t" + block[i][4] + "\t" + block[i][5] + "\t" + block[i][6] + "\t" + block[i][7]);
                //     }
                //     System.out.println();
                // }
                
                // Step 4: Inverse DCT on each block
                block = matMultiply(DCTmat_transpose, matMultiply(block, DCTmat));
                
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        block[i][j] = (int)Math.round(block[i][j]);
                    }
                }

                
                // Step 5: Transfer block data to appropriate pixel on the image
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        imChannel[block_start_idx_row + i][block_start_idx_col + j] = (int)block[i][j];
                    }
                }

            }
        }

        return imChannel;
    }


    /* HELPERS*/
    public static int[][] orderedDither(int[][] image, int[][] ditherMatrix, int imX, int imY) {
        // Set values in dither matrix to what they should be
        int[][] actualDitherMatrix = new int[ditherMatrix.length][ditherMatrix[0].length];
        int ditherMatSize = ditherMatrix.length * ditherMatrix.length;      // Square matrix
        int[][] out = new int[imY][imX];

        for (int i = 0; i < ditherMatrix.length; i++) {
            for (int j = 0; j < ditherMatrix[i].length; j++) {
                // Hard code for now
                actualDitherMatrix[i][j] = (int)((float)ditherMatrix[i][j] * (256 / ditherMatSize)); 
            }
        }

        // Sweep the image matrix with dither array
        // Case where dither matrix goes over the right or bottom of image? Maybe it doesn't matter
        int a = 0;
        int b = 0;
        for (int row = 0; row < imY; row++) {
            for (int col = 0; col < imX; col++) {
                a = row % ditherMatrix.length;          // Index of entry in dither matrix to compare with 
                b = col % ditherMatrix[a].length;

                if (image[row][col] > actualDitherMatrix[a][b]) out[row][col] = 255;
                else out[row][col] = 0;
            }
        }

        return out;
    }


    public static BufferedImage drawImageRGB(int width, int height, int[][] R, int[][] G, int[][] B) {
        // Creates a BufferedImage object with maps of the 3 color channels
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int colorRGB = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                colorRGB = B[i][j];
                colorRGB = (colorRGB << 8) + G[i][j];
                colorRGB = (colorRGB << 8) + R[i][j];

                buffered.setRGB(j, i, colorRGB);
            }
        }

        return buffered;
    }


    public static double[][] matMultiply(double[][] A, double[][] B) {
        double[][] C = new double[A.length][B[0].length];
        double[] rowA;

        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < B[0].length; j++) {
                rowA = A[i];

                for (int k = 0; k < rowA.length; k++) {
                    C[i][j] += rowA[k] * B[k][j];
                }
            }
        }

        return C;
    }


    public static double[][] matTranspose(double[][] A) {
        double[][] B =  new double[A[0].length][A.length];

        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A[0].length; j++) {
                B[j][i] = A[i][j];
            }
        }

        return B;
    }


    public static double[][] GenerateDCTTransformMatrix(int N) {
        double[][] C = new double[N][N];
        double a;

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i == 0) a = Math.sqrt(1.0/N);
                else a = Math.sqrt(2.0/N);

                C[i][j] = a * Math.cos(((2.0 * j + 1.0)*(i * Math.PI))/(2.0 * N));
            }
        }

        return C;
    }


    public static double[][] ScaleQuantizationTable(int qualityFactor, double[][] quantTable) {
        // From textbook
        double scalingFactor = 0;
        double[][] scaled_quantTable = new double[quantTable.length][quantTable[0].length];
        if (qualityFactor >= 50) scalingFactor = (100.0 - qualityFactor) / 50.0;
        else scalingFactor = 50.0 / qualityFactor;

        if (scalingFactor != 0) {
            for (int i = 0; i < quantTable.length; i++) {
                for (int k = 0; k < quantTable[0].length; k++) {
                    scaled_quantTable[i][k] = (double)Math.round(quantTable[i][k] * scalingFactor);

                    // Max is clamped to 255 for qf = 1 
                    if (scaled_quantTable[i][k] > 255) scaled_quantTable[i][k] = 255.0;
                }
            }
        } else {
            for (int i = 0; i < quantTable.length; i++) {
                for (int k = 0; k < quantTable[0].length; k++) {
                    scaled_quantTable[i][k] = 1.0;
                }
            }
        }

        return scaled_quantTable;
    }
}