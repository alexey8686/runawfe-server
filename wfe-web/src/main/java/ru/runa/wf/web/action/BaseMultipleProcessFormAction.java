package ru.runa.wf.web.action;

import com.google.common.base.Strings;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import ru.runa.common.WebResources;
import ru.runa.common.web.Messages;
import ru.runa.common.web.ProfileHttpSessionHelper;
import ru.runa.common.web.Resources;
import ru.runa.common.web.action.ActionBase;
import ru.runa.wf.web.FormSubmissionUtils;
import ru.runa.wfe.form.Interaction;
import ru.runa.wfe.task.TaskDoesNotExistException;
import ru.runa.wfe.user.Profile;
import ru.runa.wfe.user.User;
import ru.runa.wfe.validation.ValidationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import ru.runa.wfe.var.VariableProvider;

public abstract class BaseMultipleProcessFormAction extends ActionBase {
  @Override
  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
    Map<String, String> userInputErrors = null;
    ActionForward forward = null;
    try {
      User user = getLoggedUser(request);
      Profile profile = ProfileHttpSessionHelper.getProfile(request.getSession());
      // TODO fix bug when working from 2 browser tabs (token saved in
      // user session!)
      if (request.getParameter("formAjax") != null || request.getSession().getAttribute(Globals.TRANSACTION_TOKEN_KEY) == null || isTokenValid(request, true)) {
        saveToken(request);

        Long [] processId =executeProcessFromActions(request, form, mapping, profile);
        if (WebResources.isAutoShowForm()) {
          log.info("WebResources is true");
          forward = AutoShowFormHelper.getNextActionForward(user, mapping, profile, processId[0]);
          log.info("----------------------");
          log.info(forward);
          log.info("----------------------");
        }
        if (forward == null) {
          forward = mapping.findForward(Resources.FORWARD_SUCCESS);
        }
        addMessage(request, getMessage(processId));

      } else {
        forward = new ActionForward("/manage_tasks.do", true);
        log.warn(getLoggedUser(request) + " will be forwarded to tasklist due invalid token");
      }
    } catch (TaskDoesNotExistException e) {
      // In this case we must go to success forwarding, because of this
      // task is absent and form can't be displayed
      addError(request, e);
      forward = mapping.findForward(Resources.FORWARD_SUCCESS);
    } catch (ValidationException e) {
      userInputErrors = e.getConcatenatedFieldErrors("<br>");
      if (e.getGlobalErrors().size() > 0) {
        for (String message : e.getGlobalErrors()) {
          if (Strings.isNullOrEmpty(message)) {
            addError(request, new ActionMessage(Messages.MESSAGE_VALIDATION_ERROR));
          } else {
            // we are working with localized string
            addError(request, new ActionMessage(message, false));
          }
        }
      } else {
        addError(request, new ActionMessage(Messages.MESSAGE_VALIDATION_ERROR));
      }
      forward = getErrorForward(mapping, form);
    } catch (Exception e) {
      addError(request, e);
      forward = getErrorForward(mapping, form);
    }
    FormSubmissionUtils.saveUserInputErrors(request, userInputErrors);
    return forward;
  }

  protected Map<String, Object> getFormVariables(HttpServletRequest request, ActionForm actionForm,
      Interaction interaction,
      VariableProvider variableProvider) {
    return FormSubmissionUtils.extractVariables(request, actionForm, interaction, variableProvider);
  }

  protected abstract ActionMessage getMessage(Long [] processId);

  protected abstract Long [] executeProcessFromActions(HttpServletRequest request, ActionForm form, ActionMapping mapping, Profile profile);

  protected abstract ActionForward getErrorForward(ActionMapping mapping, ActionForm actionForm);
}

