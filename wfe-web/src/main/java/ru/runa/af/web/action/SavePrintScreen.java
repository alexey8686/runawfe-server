package ru.runa.af.web.action;


import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import ru.runa.common.web.action.ActionBase;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SavePrintScreen extends ActionBase {

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {

       /* final JFrame parent = new JFrame();
        JButton button = new JButton();

        button.setText("Click me to show dialog!");
        parent.add(button);
        parent.pack();
        parent.setVisible(true);

        button.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                String name = JOptionPane.showInputDialog(parent,
                        "What is your name?", null);
            }
        });*/
        JFrame parentFrame = new JFrame();
        JFileChooser fileChooser = new JFileChooser();
        parentFrame.toFront();
       parentFrame.setAlwaysOnTop(true);
        parentFrame.pack();
        parentFrame.add(fileChooser);



        fileChooser.setDialogTitle("Specify a file to save");


        int userSelection = fileChooser.showSaveDialog(parentFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            fileToSave = new File(fileToSave.getAbsolutePath() + ".jpg");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            BufferedImage screencapture = null;
            try {
                screencapture = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            } catch (AWTException e) {
                e.printStackTrace();
            }
            try {
                ImageIO.write(screencapture, "bmp", fileToSave);
            } catch (IOException e) {
                log.info(e.getMessage());
            }

        }
        return new ActionForward("/messages_page.do");
    }
}
