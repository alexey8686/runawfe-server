package ru.runa.wfe.var.dao;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.runa.wfe.commons.SystemProperties;
import ru.runa.wfe.commons.Utils;
import ru.runa.wfe.commons.dao.GenericDao2;
import ru.runa.wfe.execution.ArchivedProcess;
import ru.runa.wfe.execution.CurrentProcess;
import ru.runa.wfe.execution.Process;
import ru.runa.wfe.var.ArchivedVariable;
import ru.runa.wfe.var.BaseVariable;
import ru.runa.wfe.var.CurrentVariable;

@Component
public class VariableDao extends GenericDao2<BaseVariable, CurrentVariable, CurrentVariableDao, ArchivedVariable, ArchivedVariableDao> {

    @Autowired
    VariableDao(CurrentVariableDao dao1, ArchivedVariableDao dao2) {
        super(dao1, dao2);
    }

    /**
     * Load variables with given names for given processes.
     *
     * @param processes
     *            Processes, which variables must be loaded.
     * @param variableNames
     *            Variable names, which must be loaded for processes.
     * @return for each given process: map from variable name to loaded variable. If no variable loaded for some variable name+process, when map is
     *         still contains variable name as key, but it value is null (Result is filled completely for all processes and variable names).
     */
    public Map<Process, Map<String, BaseVariable>> getVariables(List<? extends Process> processes, List<String> variableNames) {
        if (Utils.isNullOrEmpty(processes) || Utils.isNullOrEmpty(variableNames)) {
            return null;
        }

        val result = new HashMap<Process, Map<String, BaseVariable>>();
        val currentProcesses = new ArrayList<CurrentProcess>(processes.size());
        val archivedProcesses = new ArrayList<ArchivedProcess>(processes.size());
        for (Process p : processes) {
            if (p.isArchive()) {
                archivedProcesses.add((ArchivedProcess) p);
            } else {
                currentProcesses.add((CurrentProcess) p);
            }
            val vars = new HashMap<String, BaseVariable>();
            result.put(p, vars);
            for (String name : variableNames) {
                vars.put(name, null);
            }
        }

        int databaseParametersCount = SystemProperties.getDatabaseParametersCount();
        if (!currentProcesses.isEmpty()) {
            for (val pp : Lists.partition(currentProcesses, databaseParametersCount)) {
                for (val v : dao1.getVariablesImpl(pp, variableNames)) {
                    result.get(v.getProcess()).put(v.getName(), v);
                }
            }
        }
        if (!archivedProcesses.isEmpty()) {
            for (val pp : Lists.partition(archivedProcesses, databaseParametersCount)) {
                for (val v : dao2.getVariablesImpl(pp, variableNames)) {
                    result.get(v.getProcess()).put(v.getName(), v);
                }
            }
        }

        return result;
    }
}
