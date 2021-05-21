package io.github.mayerrobert.awtbitmap;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Manifest;

public class AWTBitmap {
    private static String versionInfo;
    static {
        final ClassLoader cl = AWTBitmap.class.getClassLoader();
        final URL url = cl.getResource("META-INF/MANIFEST.MF");
        if (url == null) versionInfo = "unknown";
        else {
            try (final InputStream is = url.openStream()) {
                final Manifest manifest = new Manifest(is);
                versionInfo = manifest.getMainAttributes().getValue("Implementation-Version");
            } catch (IOException e) {
                versionInfo = "error";
            }
        }
    }
    private static final int DEEP = 256;
    private static BitmapComponent component;

    private static double CX = -0.76;
    private static double CY = 0.2;
    private static double delta = 0.02;

    public static void main(String[] argv) {
        component = new BitmapComponent(800, 600);

        final long tStart = System.currentTimeMillis();

        if (argv.length > 0 && "-j".equals(argv[0])) julia(component.img);
        else if (argv.length > 0 && "-m".equals(argv[0])) mandelbrot(component.img);
        else if (argv.length > 0 && "-c".equals(argv[0])) colors(component.img);
        else {
            System.out.println("Version " + versionInfo);
            System.out.println("Usage: java -jar awtbitmap.jar [-j|-m|-c]");
            return;
        }

        System.out.println("Done in " + (System.currentTimeMillis() - tStart) + "ms");

        java.awt.EventQueue.invokeLater(() -> {
            final Frame f = new Frame("AWT Bitmap Demo");
            f.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
            f.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    System.out.printf("x=%d, y=%d, button=%d\n", e.getX(), e.getY(), e.getButton());
                }

                @Override public void mousePressed(MouseEvent e) {}
                @Override public void mouseReleased(MouseEvent e) {}
                @Override public void mouseEntered(MouseEvent e) {}
                @Override public void mouseExited(MouseEvent e) {}
            });

            f.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                    //System.out.printf("keyTyped modifiers=%d, key=%c\n", e.getModifiers(), e.getKeyChar());
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    //System.out.printf("keyPressed modifiers=%d, key=%c\n", e.getModifiers(), e.getKeyChar());
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    final char keyChar = e.getKeyChar();
                    System.out.printf("keyReleased modifiers=%d, keyChar=%c, keyCode=%d\n", e.getModifiersEx(), keyChar, e.getKeyCode());

                    if (keyChar == '+') delta *= 2;
                    else if (keyChar == '-') delta /= 2;

                    else if (keyChar == 'x') CX -= delta;
                    else if (keyChar == 'y') CY -= delta;
                    else if (keyChar == 'X') CX += delta;
                    else if (keyChar == 'Y') CY += delta;

                    System.out.printf("CX=%g, CY=%g, delta=%g, %s\n", CX, CY, delta, Thread.currentThread().getName());

                    if (keyChar == 'x' || keyChar == 'X' || keyChar == 'y' || keyChar == 'Y') {
                        final long tStart = System.currentTimeMillis();
                        julia(component.img);
                        System.out.println("Done in " + (System.currentTimeMillis() - tStart) + "ms");
                        f.repaint();
                    }
                }
            });

            f.add(component, BorderLayout.CENTER);
            f.setLocation(50, 50);
            f.pack();
            f.setVisible(true);
        });
    }

    private static void colors(BufferedImage img) {
        for (int x = 0; x < 800; x++) {
            final int r = (int)((double)x / 800.0 * 256.0);
            for (int y = 0; y < 600; y++) {
                final int g = (int)((double)y / 600.0 * 256.0);
                final int rgb = (r<<16) + (g<<8);
                img.setRGB(x, y, rgb);
            }
        }
    }



    private static void mandelbrot(BufferedImage img) {
        java.util.stream.IntStream.range(0, 800).parallel().forEach(x -> {
        //for (int x = 0; x < 800; x++) {
            final double c = (double)x / 800.0 * 4.0 - 2.0;
            for (int y = 0; y < 600; y++) {
                final double ci = (double)y / 600.0 * 3.0 - 1.5;
                final int col = checkMandel(ci, c);
                img.setRGB(x, y, Color.HSBtoRGB((float)DEEP / col, 1, col > 0 ? 1 : 0));
            }
        });
    }

    // see https://www.hameister.org/projects_fractal.html
    private static int checkMandel(double ci, double c) {
        double zi = 0.0;
        double z  = 0.0;
        for (int i = 0; i < DEEP; i++) {
            final double ziT = 2.0 * z * zi;
            final double zT  = z * z - zi * zi;
            z = zT + c;
            zi = ziT + ci;
            if (z * z + zi * zi >= 4) {
                return DEEP-i;
            }
        }
        return 0;
    }



    private static void julia(BufferedImage img) {
        java.util.stream.IntStream.range(0, 800).parallel().forEach(x -> {
            final double c = (double)x / 800.0 * 4.0 - 2.0;
            //final double c = (double)x / 200.0 - 2.0;
            for (int y = 0; y < 600; y++) {
                final double ci = (double)y / 600.0 * 3.0 - 1.5;
                //final double ci = (double)y / 200.0 - 1.5;
                final int col = checkJulia(ci, c);
                img.setRGB(x, y, Color.HSBtoRGB((float)DEEP / col, 1, col > 0 ? 1 : 0));
            }
        });
    }

    // see https://rosettacode.org/wiki/Julia_set#Java
    // see https://de.wikipedia.org/wiki/Julia-Menge
    private static int checkJulia(double ci, double c) {
        for (int i = 0; i < DEEP; i++) {
            final double tmp = c * c - ci * ci + (CX /*-0.7*/     /*-0.4*/ /*0.285*/ /*-0.742*/ /*-0.8*/);
            ci = 2.0 * c * ci                  + (CY /* 0.27015*/ /* 0.6*/ /*0.01 */ /* 0.1  */  /*0.2*/);
            c = tmp;
            if (c * c + ci * ci >= 4) {
                return DEEP - i;
                //return i;
            }
        }
        return 0;
    }
}

class BitmapComponent extends Component {
    private static final long serialVersionUID = 1L;

    final BufferedImage img;
    
    BitmapComponent(int width, int height) {
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        setPreferredSize(new Dimension(width, height));
    }

    @Override
    public void paint(Graphics g) {
        System.out.println("paint x=" + g.getClipBounds().x + ", y=" + g.getClipBounds().y
                         + ", w=" + g.getClipBounds().width + ", h=" + g.getClipBounds().height
                         + ", " + Thread.currentThread().getName());

        final int w = getWidth();
        final int h = getHeight();

        g.drawImage(img, 0, 0, w, h, null);
    }
}
