/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import Negocios.Cifrado.Cifrado;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javazoom.upload.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lupe
 */
public class SubirArchivo extends HttpServlet {

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    javax.servlet.jsp.PageContext contextPage = null;
    javax.servlet.jsp.PageContext pageContext;
    final javax.servlet.jsp.JspFactory Factory = javax.servlet.jsp.JspFactory.getDefaultFactory();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, UploadException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            String direccion = request.getSession().getServletContext().getRealPath("ArchivosTmp/");
            javazoom.upload.UploadBean upBean = null;
            pageContext = Factory.getPageContext(this, request, response, null, true, 8192, true);
            contextPage = pageContext;
            upBean = (javazoom.upload.UploadBean) contextPage.getAttribute("upBean", javax.servlet.jsp.PageContext.PAGE_SCOPE);
            String page = null;
            String pageError = null;
            String idUsuario = null;
            String nombreArchivo = null;
            Sql sql = new Sql();
            String privada = null;

            if (upBean == null) {
                upBean = new javazoom.upload.UploadBean();
                pageContext.setAttribute("upBean", upBean, javax.servlet.jsp.PageContext.PAGE_SCOPE);
                org.apache.jasper.runtime.JspRuntimeLibrary.handleSetProperty(pageContext.findAttribute("upBean"), "folderstore", direccion);
                org.apache.jasper.runtime.JspRuntimeLibrary.introspecthelper(pageContext.findAttribute("upBean"), "whitelist", "*.cer,*.key", null, null, false);
                org.apache.jasper.runtime.JspRuntimeLibrary.introspecthelper(pageContext.findAttribute("upBean"), "overwritepolicy", "nametimestamp", null, null, false);
            }



            if (MultipartFormDataRequest.isMultipartFormData(request)) {
                MultipartFormDataRequest mrequest = new MultipartFormDataRequest(request);
                String todo = null;
                String mucho = null;
                String accion = null;

                if (mrequest != null) {
                    todo = mrequest.getParameter("todo");
                    mucho = mrequest.getParameter("mucho");
                    idUsuario = mrequest.getParameter("idUsuario");
                    privada = mrequest.getParameter("llavePrivada");
                    accion = mrequest.getParameter("accion");
                }

                if ("registro".equals(mrequest.getParameter("registro"))) {
                    page = "registro.jsp?valor=Registro";
                    pageError = "registro.jsp?valor=Error";
                } else if ("certificado".equals(mrequest.getParameter("registro"))) {
                    page = "perfil/administrarFIELyCSD.jsp?valor=cer";
                    pageError = "perfil/administrarFIELyCSD.jsp?valor=cerError";
                } else {
                    page = "perfil/administrarFIELyCSD.jsp?valor=fiel";
                    pageError = "perfil/administrarFIELyCSD.jsp?valor=fielError";
                }


                if ((todo != null) && (todo.equalsIgnoreCase("upload"))) {
                    Hashtable files = mrequest.getFiles();
                    if ((files != null) && (!files.isEmpty())) {
                        String archivo = ((UploadFile) mrequest.getFiles().get("uploadfile")).getFileName();

                        int posicionPunto = archivo.indexOf(".");
                        String nombreImagen = archivo.substring(0, posicionPunto);
                        String extension = archivo.substring(posicionPunto);
                        nombreImagen = nombreImagen + extension;
                        ((UploadFile) mrequest.getFiles().get("uploadfile")).setFileName(nombreImagen);
                        UploadFile file = (UploadFile) files.get("uploadfile");
                        if (file != null) {
                            out.println("El archivo: " + file.getFileName() + " se subio correctamente <br/>");
                            nombreArchivo = file.getFileName();
                        }
                        upBean.store(mrequest, "uploadfile");
                    } else {
                        out.println("Archivos CER no subidos");
                    }

                    /*
                     * Guardar Archivo
                     */
                    String sentencia = "INSERT INTO csd VALUES(?,?,?,?)";
                    File archivoCSD = new File(direccion + "/" + nombreArchivo);
                    InputStream ISCSD = new FileInputStream(archivoCSD);
                    byte[] bufferCSD = new byte[(int) archivoCSD.length()];
                    int offset = 0;
                    int numRead = 0;
                    while (offset < bufferCSD.length && (numRead = ISCSD.read(bufferCSD, offset, bufferCSD.length - offset)) >= 0) {
                        offset += numRead;
                    }
                    ISCSD.close();
                    String noCertificado = null;
                    noCertificado = Cifrado.obtenerNumeroCertificado(bufferCSD);
                    if (noCertificado != null) {
                        if ("modificar".equals(accion)) {
                            sql.actualizarCSD(archivoCSD, Cifrado.decodificarBase64(idUsuario),noCertificado);
                        } else {
                            sql.insertarCsd(sentencia, noCertificado, archivoCSD, Integer.parseInt(idUsuario));
                            out.println(".Archivo CSD en base");
                        }
                    } else {
                        out.println(".No se puede obtener noCetificado");
                    }



                }

                /*
                 * ARCHIVO FIEL
                 */
                if ((mucho != null) && (mucho.equalsIgnoreCase("upload"))) {
                    Hashtable archivos = mrequest.getFiles();
                    if ((archivos != null) && (!archivos.isEmpty())) {
                        String fiel = ((UploadFile) mrequest.getFiles().get("fileupload")).getFileName();

                        int posicionPunto = fiel.indexOf(".");
                        String nombreImagen = fiel.substring(0, posicionPunto);
                        String extension = fiel.substring(posicionPunto);
                        nombreImagen = nombreImagen + extension;
                        ((UploadFile) mrequest.getFiles().get("fileupload")).setFileName(nombreImagen);
                        UploadFile Fiel = (UploadFile) archivos.get("fileupload");
                        if (Fiel != null) {
                            out.println("El archivo: " + Fiel.getFileName() + " se subio correctamente");
                            nombreArchivo = Fiel.getFileName();
                        }
                        upBean.store(mrequest, "fileupload");
                    } else {
                        out.println("Archivos FIEL no subidos");
                    }

                    /*
                     * Guardar Archivo
                     */

                    File archivoFiel = new File(direccion + "/" + nombreArchivo);
                    InputStream ISFiel = new FileInputStream(archivoFiel);
                    byte[] bufferFiel = new byte[(int) archivoFiel.length()];
                    int offset = 0;
                    int numRead = 0;
                    
                    while (offset < bufferFiel.length && (numRead = ISFiel.read(bufferFiel, offset, bufferFiel.length - offset)) >= 0) {
                        offset += numRead;
                    }
                    ISFiel.close();
                    PrivateKey keyFiel = null;
                    keyFiel = Cifrado.getLlavePrivada(bufferFiel, privada);
                    

                    if (keyFiel != null) {
                        if ("modificar".equals(accion)) {
                            sql.actualizarFiel(archivoFiel, Cifrado.decodificarBase64(idUsuario));
                            archivoFiel.delete();
                            out.println("<br/> Actualizardo Fiel " + Cifrado.decodificarBase64(idUsuario));
                        } else {
                            String sentencia = "INSERT INTO fiel VALUES(?,?,?)";
                            sql.insertarFiel(sentencia, archivoFiel, Integer.parseInt(idUsuario));
                            out.println("<br/>Archivo fiel en base");
                        }
                    } else {
                        out.println("Error");
                        response.sendRedirect(pageError);
                    }
                }
            }

            response.sendRedirect(page);


        } catch (InstantiationException ex) {
            Logger.getLogger(SubirArchivo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(SubirArchivo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(SubirArchivo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SubirArchivo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(SubirArchivo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchProviderException ex) {
            Logger.getLogger(SubirArchivo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(SubirArchivo.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (UploadException ex) {
            Logger.getLogger(SubirArchivo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (UploadException ex) {
            Logger.getLogger(SubirArchivo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
