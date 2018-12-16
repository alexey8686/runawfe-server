package ru.runa.wf.web.form;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import ru.runa.common.web.MessagesException;

public class MultipleProcessForm extends ActionForm {

  private static final long serialVersionUID = 1L;

  public static final String ID_INPUT_NAME = "id";


  private String id;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
    ActionErrors errors = new ActionErrors();
    if (getId() == null) {
      errors.add(ActionMessages.GLOBAL_MESSAGE,
          new ActionMessage(MessagesException.ERROR_NULL_VALUE.getKey()));
    }
    return errors;
  }
}

