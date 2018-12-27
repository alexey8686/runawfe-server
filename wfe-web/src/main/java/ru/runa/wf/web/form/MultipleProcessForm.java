package ru.runa.wf.web.form;

import org.apache.struts.action.*;
import ru.runa.common.web.Messages;

import javax.servlet.http.HttpServletRequest;

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
      errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(Messages.ERROR_NULL_VALUE));
    }
    return errors;
  }
}
