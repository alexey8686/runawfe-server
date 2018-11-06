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

        JFrame jFrame = new JFrame();
        JFileChooser fileChooser = new JFileChooser();

        jFrame.toFront();
        jFrame.setAlwaysOnTop(true);
        fileChooser.setDialogTitle("Specify a file to save");
        int userSelection = fileChooser.showSaveDialog(jFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            fileToSave = new File(fileToSave.getAbsolutePath() + ".jpg");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error(e.getMessage(),e.getCause());
            }
            BufferedImage screencapture = null;
            try {
                screencapture = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            } catch (AWTException e) {
                log.error(e.getMessage(),e.getCause());
                return null;
            }
            try {
                ImageIO.write(screencapture, "jpg", fileToSave);
            } catch (IOException e) {
                log.error(e.getMessage(),e.getCause());
                return null;
            }

        }
        return new ActionForward("/WEB-INF/af/print.jsp");
    }
}
