import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.process.ImageProcessor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Calendar;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;

/*
 * The MIT License
 *
 * Copyright 2016 Takehito Nishida.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Grab an image from IP Webcam.
 * https://play.google.com/store/apps/details?id=com.pas.webcam&hl=ja
 */
public class WK_GrabFromIpWebcam implements ExtendedPlugInFilter
{
    // const var.
    public static final String VERSION = "0.9.2.0";
    private final int FLAGS = NO_IMAGE_REQUIRED;
    private final String HTTP = "http://";
    private final String[] STR_ORIENTATIONS = new String[] { "landscape", "upsidedown", "portrait", "upsidedown_portrait"};

    // static var.
    private static String ip1 = "192";
    private static String ip2 = "168";
    private static String ip3 = "255";
    private static String ip4 = "255";
    private static String port = "8080";
    private static int wait_time = 100;

    // var.
    private ImagePlus imp_dsp = new ImagePlus();
    private ImagePlus imp_url = new ImagePlus();
    private String title = null;
    private String url_base = null;
    private double framenum = 0.0;

    public JDialog diag_cntrl = null;
    private boolean flag_fin_loop = false;
    private boolean flag_still = false;
    private boolean flag_stillaf = false;
    private boolean flag_focus = false;
    private boolean isFocus = true;
    private boolean flag_led = false;
    private boolean onLed = false;
    private boolean flag_ffc = false;
    private boolean isFfc = false;
    private boolean flag_night_vision = false;
    private boolean isNightVision = false;
    private boolean flag_overlay = false;
    private boolean isOverlay = false;
    private boolean flag_orientation = false;
    private int ind_orientation = 0;
    private boolean flag_enDisplayStill = false;
    private boolean enDisplayStill = false;

    @Override
    public int showDialog(ImagePlus arg0, String cmd, PlugInFilterRunner arg2)
    {
        title = cmd.trim();
        GenericDialog gd = new GenericDialog(title + "...");

        gd.addStringField("ip1", ip1);
        gd.addStringField("ip2", ip2);
        gd.addStringField("ip3", ip3);
        gd.addStringField("ip4", ip4);
        gd.addStringField("port", port);
        gd.addNumericField("wait_time", wait_time, 0);

        gd.showDialog();

        if (gd.wasCanceled())
        {
            return DONE;
        }
        else
        {
            ip1 = gd.getNextString();
            ip2 = gd.getNextString();
            ip3 = gd.getNextString();
            ip4 = gd.getNextString();
            port = gd.getNextString();
            wait_time = (int)gd.getNextNumber();

            return FLAGS;
        }
    }

    @Override
    public void setNPasses(int arg0)
    {
        // do nothing
    }

    @Override
    public int setup(String arg0, ImagePlus arg1)
    {
        return FLAGS;
    }

    @Override
    public void run(ImageProcessor arg0)
    {
        // ----- The control dialog during continuous grabbing -----
        diag_cntrl = new JDialog(diag_cntrl, title, false);
        final JButton but_stop_cont = new JButton("Stop");
        final JButton but_still_cont = new JButton("Create still image");
        final JButton but_stillaf_cont = new JButton("Create AF still image");
        final JButton but_focus_cont = new JButton("Focus");
        final JButton but_led_cont = new JButton("LED is off");
        final JButton but_ffc_cont = new JButton("RearCamera");
        final JButton but_nv_cont = new JButton("NightVision is off");
        final JButton but_ovl_cont = new JButton("Overlay is off");
        final JButton but_ori_cont = new JButton(STR_ORIENTATIONS[ind_orientation]);
        final JButton but_dspstill_cont = new JButton("Display latest frame");

        but_stop_cont.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                flag_fin_loop = true;
                diag_cntrl.dispose();
            }
        });

        but_still_cont.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                flag_still = true;
            }
        });

        but_stillaf_cont.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                flag_stillaf = true;
            }
        });
        
        but_focus_cont.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(isFocus)
                {
                    but_focus_cont.setText("No focus");
                    isFocus = false;
                }
                else
                {
                    but_focus_cont.setText("Focus");
                    isFocus = true;
                }

                flag_focus = true;
            }
        });

        but_led_cont.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(onLed)
                {
                    but_led_cont.setText("LED is off");
                    onLed = false;
                }
                else
                {
                    but_led_cont.setText("LED is on");
                    onLed = true;
                }

                flag_led = true;
            }
        });

        but_ffc_cont.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(isFfc)
                {
                    but_ffc_cont.setText("RearCamera");
                    isFfc = false;
                }
                else
                {
                    but_ffc_cont.setText("FrontCamera");
                    isFfc = true;
                }

                flag_ffc = true;
            }
        });

        but_nv_cont.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(isNightVision)
                {
                    but_nv_cont.setText("NightVision is off");
                    isNightVision = false;
                }
                else
                {
                    but_nv_cont.setText("NightVision is on");
                    isNightVision = true;
                }

                flag_night_vision = true;
            }
        });

        but_ovl_cont.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(isOverlay)
                {
                    but_ovl_cont.setText("Overlay is off");
                    isOverlay = false;
                }
                else
                {
                    but_ovl_cont.setText("Overlay is on");
                    isOverlay = true;
                }

                flag_overlay = true;
            }
        });

        but_ori_cont.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ind_orientation++;

                if(STR_ORIENTATIONS.length <= ind_orientation)
                {
                    ind_orientation = 0;
                }

                but_ori_cont.setText(STR_ORIENTATIONS[ind_orientation]);
                flag_orientation = true;
            }
        });

        but_dspstill_cont.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(enDisplayStill)
                {
                    but_dspstill_cont.setText("Display latest frame");
                    enDisplayStill = false;
                }
                else
                {
                    but_dspstill_cont.setText("Display still image");
                    enDisplayStill = true;
                }

                flag_enDisplayStill = true;
            }
        });

        diag_cntrl.addWindowListener(new WindowAdapter() {
              @Override
              public void windowClosing(WindowEvent e) {
                  flag_fin_loop = true;
                  diag_cntrl.dispose();
              }
        });

        diag_cntrl.setLayout(new GridLayout(10, 1));
        diag_cntrl.add(but_dspstill_cont);
        diag_cntrl.add(but_stop_cont);
        diag_cntrl.add(but_ovl_cont);
        diag_cntrl.add(but_ffc_cont);
        diag_cntrl.add(but_ori_cont);
        diag_cntrl.add(but_focus_cont);
        diag_cntrl.add(but_nv_cont);
        diag_cntrl.add(but_led_cont);
        diag_cntrl.add(but_still_cont);
        diag_cntrl.add(but_stillaf_cont);
        diag_cntrl.setSize(200, 400);
        diag_cntrl.setVisible(true);
        // ----- End of the control dialog -----
        
        try
        {
            // Set URLs
            if(ip2.equals("") || ip3.equals("") || ip4.equals("") || port.equals(""))
            {
                url_base = ip1;
                
                if(url_base.endsWith("/"))
                {
                    int num = url_base.length();
                    url_base = url_base.substring(0, num - 1);
                }
            }
            else
            {
                url_base = HTTP + ip1 + "." + ip2 + "." + ip3 + "." + ip4 + ":" + port;
            }

            URL url_shot  = new URL(url_base + "/shot.jpg");
            URL url_still = new URL(url_base + "/photo.jpg");
            URL url_stillaf = new URL(url_base + "/photoaf.jpg");
            URL url_focus = new URL(url_base + "/focus");
            URL url_nofocus = new URL(url_base + "/nofocus");
            URL url_led_on = new URL(url_base + "/enabletorch");
            URL url_led_off = new URL(url_base + "/disabletorch");
            URL url_ffc_on = new URL(url_base + "/settings/ffc?set=on");
            URL url_ffc_off = new URL(url_base + "/settings/ffc?set=off");
            URL url_nv_on = new URL(url_base + "/settings/night_vision?set=on");
            URL url_nv_off = new URL(url_base + "/settings/night_vision?set=off");
            URL url_ovl_on = new URL(url_base + "/settings/overlay?set=on");
            URL url_ovl_off = new URL(url_base + "/settings/overlay?set=off");
            URL url_ori = new URL(url_base + "/settings/orientation?set=" + STR_ORIENTATIONS[ind_orientation]);
            
            // Initialize IP Webcam
            connectUrl(url_focus);
            connectUrl(url_led_off);
            connectUrl(url_ffc_off);
            connectUrl(url_nv_off);
            connectUrl(url_ovl_off);
            connectUrl(url_ori);
            
            // Initialize the display window
            setImageFromUrl(url_shot);
            
            // run
            long time_ini = Calendar.getInstance().getTimeInMillis();
 
            for(;;)
            {
                if(flag_fin_loop)
                {
                    break;
                }
                
                // grab
                if(enDisplayStill)
                {
                    setImageFromUrl(url_still);
                }
                else
                {
                    setImageFromUrl(url_shot);
                }
                
                // stopwatch
                framenum = framenum + 1.0;
                long time_diff = Calendar.getInstance().getTimeInMillis() - time_ini;
                double framerate = 0;

                if (0 < time_diff)
                {
                    framerate = (double) framenum * 1000;
                    framerate = framerate / time_diff;
                }

                IJ.showStatus(String.format("%.1f fps", framerate));

                // still
                if(flag_still)
                {
                    ImagePlus impStill = createImagePlusFromUrl(url_still);
                    impStill.setTitle(url_still.getPath().replace("/", ""));
                    impStill.show();
                    flag_still = false;
                }

                if(flag_stillaf)
                {
                    ImagePlus impStillaf = createImagePlusFromUrl(url_stillaf);
                    impStillaf.setTitle(url_stillaf.getPath().replace("/", ""));
                    impStillaf.show();
                    flag_stillaf = false;
                }

                // function
                if(flag_focus)
                {
                    if(isFocus)
                    {
                        connectUrl(url_focus);
                    }
                    else
                    {
                        connectUrl(url_nofocus);
                    }                    
                    
                    flag_focus = false;
                }

                if(flag_led)
                {
                    if(onLed)
                    {
                        connectUrl(url_led_on);
                    }
                    else
                    {
                        connectUrl(url_led_off);
                    }

                    flag_led = false;
                }

                if(flag_ffc)
                {
                    if(isFfc)
                    {
                        connectUrl(url_ffc_on);
                    }
                    else
                    {
                        connectUrl(url_ffc_off);
                    }

                    time_ini = Calendar.getInstance().getTimeInMillis();
                    framenum = 0.0;
                    flag_ffc = false;
                }

                if(flag_night_vision)
                {
                    if(isNightVision)
                    {
                        connectUrl(url_nv_on);
                    }
                    else
                    {
                        connectUrl(url_nv_off);
                    }

                    flag_night_vision = false;
                }

                if(flag_overlay)
                {
                    if(isOverlay)
                    {
                        connectUrl(url_ovl_on);
                    }
                    else
                    {
                        connectUrl(url_ovl_off);
                    }

                    flag_overlay = false;
                }

                if(flag_orientation)
                {
                    url_ori = new URL(url_base + "/settings/orientation?set=" + STR_ORIENTATIONS[ind_orientation]);
                    connectUrl(url_ori);
                    imp_dsp.close();
                    flag_orientation = false;
                }

                if( flag_enDisplayStill)
                {
                    time_ini = Calendar.getInstance().getTimeInMillis();
                    framenum = 0.0;
                    imp_dsp.close();
                    flag_enDisplayStill = false;
                }

                // redraw
                if(imp_dsp.getProcessor() != null &&  !imp_dsp.isVisible())
                {
                    imp_dsp.setTitle(title);
                    imp_dsp.show();
                }

                if(imp_dsp.getProcessor() != null)
                {
                    imp_dsp.draw();
                }
                
                // wait
                wait(wait_time);
            }
        }
        catch(Exception ex)
        {
            IJ.error(ex.getMessage());
            diag_cntrl.dispose();
            return;
        }
        
        diag_cntrl.dispose();
    }

    private void wait(int wt){

        try
        {
            if(wt != 0)
            {
                Thread.sleep(wt);
            }
        } 
        catch (InterruptedException e)
        {
            // do nothing
        }
    }

    private void setImageFromUrl (URL url) throws Exception
    {
        try
        {           
            imp_url = null;
            imp_url = createImagePlusFromUrl(url);
            
            if(imp_url == null)
            {
                throw new Exception("Can not create ImagePlus from url.");
            }
            
            if(imp_dsp.getProcessor() == null || imp_url.getWidth() != imp_dsp.getWidth() || imp_url.getHeight() != imp_dsp.getHeight())
            {
                imp_dsp = null;
                imp_dsp = new ImagePlus();
                imp_dsp.setImage(imp_url);
            }
            else
            {
                int[] impdst_intarray = (int[])imp_dsp.getChannelProcessor().getPixels();
                int[] impsht_intarray = (int[])imp_url.getChannelProcessor().getPixels();
                int numpix = imp_url.getWidth() * imp_url.getHeight();
                System.arraycopy(impsht_intarray, 0, impdst_intarray, 0, numpix);        
            }
        }
        catch(Exception ex)
        {
            throw new Exception("Error of setImageFromUrl() :" + ex.getMessage());
        }
    }
    
    private ImagePlus createImagePlusFromUrl(URL url)
    {
        try
        {
            ImagePlus impGrab = null;
            BufferedImage buf_img = ImageIO.read(url);

            if(buf_img == null)
            {
                throw new Exception("dummy");
            }
            
            impGrab = new ImagePlus("buf_img", buf_img);
            int w = impGrab.getWidth();
            int h = impGrab.getHeight();

            if(w == 0 || h == 0)
            {
                throw new Exception("dummy");
            }

            return impGrab;
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private void connectUrl(URL url) throws Exception
    {
        try
        {
            HttpURLConnection uc = (HttpURLConnection)url.openConnection();
            uc.setRequestMethod("GET");
            uc.setInstanceFollowRedirects(false);
            uc.setRequestProperty("Accept-Language", "en");
            uc.connect();
            String res = uc.getResponseMessage();
            uc.disconnect();
            uc = null;

            if(!res.equals("OK"))
            {
                throw new Exception("The response is NG");
            }
        }
        catch(Exception ex)
        {
            throw new Exception("Error of connectUrl() :" + ex.getMessage());
        }
    }
}
