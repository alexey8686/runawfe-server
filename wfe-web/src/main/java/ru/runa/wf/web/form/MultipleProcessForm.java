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
  public static final String ACTOR_ID_INPUT_NAME = "actorId";
  public static final String ID_INPUT_NAME = "id";

  private Long actorId;
  private String submitButton;
  private boolean multipleSubmit;
  private String id;
  private Long[] ID;

  public Long getActorId() {
    return actorId;
  }

  public void setActorId(Long actorId) {
    this.actorId = actorId;
  }

  public String getSubmitButton() {
    if (multipleSubmit) {
      return submitButton;
    }
    return null;
  }

  public void setSubmitButton(String submitButton) {
    this.submitButton = submitButton;
  }

  public boolean isMultipleSubmit() {
    return multipleSubmit;
  }

  public void setMultipleSubmit(boolean multipleSubmit) {
    this.multipleSubmit = multipleSubmit;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  /*  String[] array = id.split(",");
    Long[] longArray = new Long[array.length];
    for (int i = 0; i < array.length; i++) {
      longArray[i] = Long.parseLong(array[i]);
    }
    setID(longArray);*/
  }

  public Long[] getID() {
    return ID;
  }

  public void setID(Long[] ID) {
    this.ID = ID;
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

