/* Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.getstarted.basicactions;

// [START bookshelf_create_servlet]

import com.example.getstarted.daos.BookDao;
import com.example.getstarted.objects.Book;
import com.example.getstarted.util.CloudStorageHelper;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

@SuppressWarnings("serial")
@WebServlet(
    name = "create",
    urlPatterns = {"/create"})
public class CreateBookServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(CreateBookServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    req.setAttribute("action", "Add");
    req.setAttribute("destination", "create");
    req.setAttribute("page", "form");
    req.getRequestDispatcher("/base.jsp").forward(req, resp);
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    assert ServletFileUpload.isMultipartContent(req);
    CloudStorageHelper storageHelper =
        (CloudStorageHelper) getServletContext().getAttribute("storageHelper");

    String newImageUrl = null;
    Map<String, String> params = new HashMap<String, String>();
    try {
      FileItemIterator iter = new ServletFileUpload().getItemIterator(req);
      while (iter.hasNext()) {
        FileItemStream item = iter.next();
        if (item.isFormField()) {
          params.put(item.getFieldName(), Streams.asString(item.openStream()));
        } else if (!Strings.isNullOrEmpty(item.getName())) {
          newImageUrl =
              storageHelper.uploadFile(
                  item, System.getenv("BOOKSHELF_BUCKET"));
        }
      }
    } catch (FileUploadException e) {
      throw new IOException(e);
    }

    String createdByString = "";
    String createdByIdString = "";
    HttpSession session = req.getSession();
    if (session.getAttribute("userEmail") != null) { // Does the user have a logged in session?
      createdByString = (String) session.getAttribute("userEmail");
      createdByIdString = (String) session.getAttribute("userId");
    }

    Book book =
        new Book.Builder()
            .author(params.get("author"))
            .description(params.get("description"))
            .publishedDate(params.get("publishedDate"))
            .title(params.get("title"))
            .imageUrl(null == newImageUrl ? params.get("imageUrl") : newImageUrl)
            .createdBy(createdByString)
            .createdById(createdByIdString)
            .build();

    BookDao dao = (BookDao) this.getServletContext().getAttribute("dao");
    String id = dao.createBook(book);
    logger.log(Level.INFO, "Created book {0}", book);
    resp.sendRedirect("/read?id=" + id);
  }
}
// [END bookshelf_create_servlet]
