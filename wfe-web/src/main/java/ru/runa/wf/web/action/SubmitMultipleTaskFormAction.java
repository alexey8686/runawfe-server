package ru.runa.wf.web.action;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.springframework.transaction.annotation.Transactional;
import ru.runa.common.WebResources;
import ru.runa.common.web.Commons;
import ru.runa.common.web.JsonConverter;
import ru.runa.common.web.Messages;
import ru.runa.wf.web.FormSubmissionUtils;
import ru.runa.wf.web.form.MultipleProcessForm;
import ru.runa.wf.web.form.ProcessForm;
import ru.runa.wfe.execution.dto.WfProcess;
import ru.runa.wfe.form.Interaction;
import ru.runa.wfe.service.client.DelegateProcessVariableProvider;
import ru.runa.wfe.service.delegate.Delegates;
import ru.runa.wfe.service.impl.TaskServiceBean;
import ru.runa.wfe.task.dto.WfTask;
import ru.runa.wfe.user.Profile;
import ru.runa.wfe.user.User;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created on 17.12.2018
 *
 * Using for multiple forms confirm, obtain MultipleProcessForm with Json string in param id.
 *
 * @struts:action path="/submitMultipleTaskForm" name="multipleProcessForm" validate="true" input =
 * "/WEB-INF/wf/manage_tasks.jsp"
 * @struts.action-forward name="success" path="/manage_tasks.do" redirect = "true"
 * @struts.action-forward name="failure" path="/submit_task.do" redirect = "false"
 * @struts.action-forward name="submitTask" path="/submit_task.do" redirect = "false"
 * @struts.action-forward name="tasksList" path="/manage_tasks.do" redirect = "true"
 */
public class SubmitMultipleTaskFormAction extends BaseMultipleProcessFormAction {
  /*Выполнение узла*/

  TaskServiceBean taskServiceBean;

  @Override
  protected Long[] executeProcessFromActions(HttpServletRequest request, ActionForm actionForm,
      ActionMapping mapping, Profile profile) {

    try {

      taskServiceBean = (TaskServiceBean) new InitialContext().lookup(
          "java:global/runawfe/wfe-service-4-SNAPSHOT/TaskServiceBean!ru.runa.wfe.service.decl.TaskServiceLocal");

    } catch (NamingException e) {
      e.printStackTrace();
    }
    List<Long> taskIds = new ArrayList<>();
    List<Map<String, Object>> veriablesList = new ArrayList<>();
    List<Long> actorIds = new ArrayList<>();
    User user = getLoggedUser(request);

    MultipleProcessForm multipleProcessForm = (MultipleProcessForm) actionForm;
    List<ProcessForm> forms = JsonConverter
        .getFormList(new ProcessForm(), multipleProcessForm.getMultipartRequestHandler(),
            multipleProcessForm.getId());
    Long[] processId = new Long[forms.size()];

    int i = 0;
    for (ProcessForm form : forms) {
      //Получение task_id из
      Long taskId = form.getId();

      //Запись в лог
      log.debug(user + " submitted task form for id " + taskId);
      WfTask task = Delegates.getTaskService().getTask(user, taskId);
      processId[i++] = task.getProcessId();
      log.info("PROCESS ID IS -" + task.getProcessId());
      //Получение взаимодействие узла и пользователя
      Interaction interaction = Delegates.getDefinitionService()
          .getTaskNodeInteraction(user, task.getDefinitionId(), task.getNodeId());

      //Список параметров
      Map<String, Object> variables = getFormVariables(request, actionForm, interaction,
          new DelegateProcessVariableProvider(user, task.getProcessId()));

      //Получаем task из базы по пользователю и task_id, так же получаем process_id

      //Получение названия перехода
      String transitionName = form.getSubmitButton();

      // Добавляем в список переменных переменную RUNAWFE_SELECTED_TRANSITION со значение перехода
      variables.put(WfProcess.SELECTED_TRANSITION_KEY, transitionName);

      taskIds.add(taskId);
      veriablesList.add(variables);
      actorIds.add(form.getActorId());

      //Выполнение узла
      taskServiceBean.completeMultiplTask(user, taskIds, veriablesList, actorIds);

    }

    return processId;
  }

  @Override
  protected ActionForward getErrorForward(ActionMapping mapping, ActionForm actionForm) {
    MultipleProcessForm form = (MultipleProcessForm) actionForm;
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(ProcessForm.ID_INPUT_NAME, form.getId());
    return Commons
        .forward(mapping.findForward(ru.runa.common.web.Resources.FORWARD_FAILURE), params);
  }

  @Override
  protected ActionMessage getMessage(Long[] processId) {
    return new ActionMessage(Messages.TASK_COMPLETED, Arrays.toString(processId));
  }
}
