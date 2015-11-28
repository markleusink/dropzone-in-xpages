package eu.linqed;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lotus.domino.*;
import com.ibm.xsp.http.UploadedFile;
import com.ibm.xsp.webapp.XspHttpServletResponse;

public class UploadHandler {

	private static String FILE_PARAM = "uploadedFile"; // name of the multipart
	// POST param that holds
	// the uploaded file
	private static String RT_ITEM_NAME_FILES = "file"; // name of the RT item

	// that will hold the
	// uploaded file

	public UploadHandler() {
	}

	@SuppressWarnings("unchecked")
	public static void process() {

		XspHttpServletResponse response = null;
		PrintWriter pw = null;

		UploadedFile uploadedFile = null;
		File correctedFile = null;

		RichTextItem rtFiles = null;
		Document doc = null;

		String fileName = "";

		FacesContext facesContext = FacesContext.getCurrentInstance();

		try {

			ExternalContext extCon = facesContext.getExternalContext();
			response = (XspHttpServletResponse) extCon.getResponse();
			pw = response.getWriter();

			//only HTTP POST is allowed
			HttpServletRequest request = (HttpServletRequest) extCon.getRequest();
			if (!request.getMethod().equalsIgnoreCase("post")) {
				throw (new Exception("only POST is allowed"));
			}

			Database dbCurrent = (Database) resolveVariable("database");

			//set up output object
			response.setContentType("text/plain");
			response.setHeader("Cache-Control", "no-cache");
			response.setDateHeader("Expires", -1);

			//check if we have a file in the POST
			Map map = request.getParameterMap();

			if (!map.containsKey(FILE_PARAM)) {
				throw (new Exception("no file received"));
			}

			//get the file from the request
			uploadedFile = (UploadedFile) map.get(FILE_PARAM);

			if (uploadedFile == null) {
				throw (new Exception("that's not a file!"));
			}

			//store file in a document
			fileName = uploadedFile.getClientFileName(); //original name of the file

			File tempFile = uploadedFile.getServerFile(); // the uploaded file with a cryptic name

			//we rename the file to its original name, so we can attach it with that name
			//see http://www.bleedyellow.com/blogs/m.leusink/entry/processing_files_uploaded_to_an_xpage?lang=nl
			correctedFile = new java.io.File(tempFile.getParentFile().getAbsolutePath() + java.io.File.separator + fileName);
			boolean renamed = tempFile.renameTo(correctedFile);

			if (renamed) {

				//create a document in the current db
				doc = dbCurrent.createDocument();
				doc.replaceItemValue("form", "fFile");

				//attach file to target document
				rtFiles = doc.createRichTextItem(RT_ITEM_NAME_FILES);
				rtFiles.embedObject(lotus.domino.EmbeddedObject.EMBED_ATTACHMENT, "", correctedFile.getAbsolutePath(), null);

				boolean saved = doc.save();

			}

			pw.print("add code to return to the upload method here");

			response.commitResponse();

		} catch (Exception e) {

			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

			pw.print("add code here to return an error");

			try {
				response.commitResponse();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		} finally {

			facesContext.responseComplete();

			try {

				if (rtFiles != null) {
					rtFiles.recycle();
				}
				if (doc != null) {
					doc.recycle();
				}

				if (correctedFile != null) {
					// rename temporary file back to its original name so it's
					// automatically
					// deleted by the XPages engine
					correctedFile.renameTo(uploadedFile.getServerFile());
				}
			} catch (Exception ee) {
				ee.printStackTrace();
			}

		}
	}

	private static Object resolveVariable(String variableName) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		return facesContext.getApplication().getVariableResolver().resolveVariable(facesContext, variableName);
	}

}