package ru.runa.af.web.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import ru.runa.common.WebResources;
import ru.runa.common.web.Commons;
import ru.runa.wf.web.FormSubmissionUtils;
import ru.runa.wf.web.MessagesProcesses;
import ru.runa.wf.web.action.BaseProcessFormAction;
import ru.runa.wf.web.form.MultipleProcessForm;
import ru.runa.wf.web.form.ProcessForm;
import ru.runa.wfe.execution.dto.WfProcess;
import ru.runa.wfe.form.Interaction;
import ru.runa.wfe.service.client.DelegateProcessVariableProvider;
import ru.runa.wfe.service.delegate.Delegates;
import ru.runa.wfe.task.dto.WfTask;
import ru.runa.wfe.user.Profile;
import ru.runa.wfe.user.User;

public class SubmitMultipleTaskFormAction extends BaseProcessFormAction {


  @Override
  protected Long executeProcessFromAction(HttpServletRequest request, ActionForm actionForm,
      ActionMapping mapping, Profile profile) {
    User user = getLoggedUser(request);
    MultipleProcessForm form = (MultipleProcessForm) actionForm;
    JSONParser parser = new JSONParser();

    JSONArray array = null;
    ObjectMapper objectMapper=null;
    try {
      array = (JSONArray) parser.parse(form.getId());
      objectMapper = new ObjectMapper();
      for(Object obj:array) {
        JSONObject jsonObject = (JSONObject) obj;
        ProcessForm processForm=(ProcessForm)objectMapper.readValue(jsonObject.toJSONString(),ProcessForm.class);
        Long id=processForm.getId();
      }
    } catch (ParseException e) {
      e.printStackTrace();
    }
      catch (IOException e) {
        e.printStackTrace();
      }





    System.out.println();
    Long processId = null;
    for (int i = 0; i < form.getID().length; i++) {

      Long taskId = form.getID()[i];
      log.debug(user + " submitted task form for id " + taskId);
      WfTask task = Delegates.getTaskService().getTask(user, taskId);
      Interaction interaction = Delegates.getDefinitionService()
          .getTaskNodeInteraction(user, task.getDefinitionId(), task.getNodeId());
      Map<String, Object> variables = getFormVariables(request, actionForm, interaction,
          new DelegateProcessVariableProvider(user, task.getProcessId()),taskId);

      if (WebResources.isAutoShowForm()) {
        processId = task.getProcessId();
      }
      String transitionName = form.getSubmitButton();
      variables.put(WfProcess.SELECTED_TRANSITION_KEY, transitionName);
      Delegates.getTaskService().completeTask(user, taskId, variables, form.getActorId());
      FormSubmissionUtils.clearUserInputFiles(request);


    }
    return processId;
  }

  @Override
  protected ActionForward getErrorForward(ActionMapping mapping, ActionForm actionForm) {
    ProcessForm form = (ProcessForm) actionForm;
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(ProcessForm.ID_INPUT_NAME, form.getId());
    return Commons
        .forward(mapping.findForward(ru.runa.common.web.Resources.FORWARD_FAILURE), params);
  }

  @Override
  protected ActionMessage getMessage(Long processId) {
    return new ActionMessage(MessagesProcesses.TASK_COMPLETED.getKey());
  }
}

