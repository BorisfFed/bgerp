package ru.bgcrm.struts.action.admin;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.w3c.dom.Element;

import ru.bgcrm.cache.ParameterCache;
import ru.bgcrm.cache.ProcessTypeCache;
import ru.bgcrm.dao.CommonDAO;
import ru.bgcrm.dao.ParamDAO;
import ru.bgcrm.dao.ParamGroupDAO;
import ru.bgcrm.dao.PatternDAO;
import ru.bgcrm.model.Customer;
import ru.bgcrm.model.ListItem;
import ru.bgcrm.model.SearchResult;
import ru.bgcrm.model.param.Parameter;
import ru.bgcrm.model.param.ParameterGroup;
import ru.bgcrm.model.param.Pattern;
import ru.bgcrm.model.param.address.AddressHouse;
import ru.bgcrm.model.process.Process;
import ru.bgcrm.model.process.ProcessType;
import ru.bgcrm.model.user.User;
import ru.bgcrm.plugin.Plugin;
import ru.bgcrm.plugin.PluginManager;
import ru.bgcrm.struts.action.BaseAction;
import ru.bgcrm.struts.form.DynActionForm;
import ru.bgcrm.util.Utils;
import ru.bgcrm.util.XMLUtils;

public class DirectoryAction extends BaseAction {
    protected String getConfigString(Map<String, String> configMap) {
        StringBuilder config = new StringBuilder();
        if (configMap != null) {
            for (String key : configMap.keySet()) {
                config.append(key);
                config.append("=");
                config.append(configMap.get(key));
                config.append("\n");
            }
        }
        return config.toString();
    }

    private List<ListItem> directoryList = null;
    private static Map<String, String> directoryMap = new HashMap<String, String>();
    static {
        directoryMap.put("customerParameter", "Параметры контрагентов");
        directoryMap.put("customerParameterGroup", "Группы параметров контрагентов");
        directoryMap.put("customerPatternTitle", "Шаблоны названия контрагентов");
        directoryMap.put("processParameter", "Параметры процессов");
        directoryMap.put("userParameter", "Параметры пользователей");
        directoryMap.put("addressParameter", "Параметры домов");

        // добавляем сущности из xml-конфигов плагинов
        PluginManager pluginManager = PluginManager.getInstance();
        for (Plugin plugin : pluginManager.getPluginList()) {
            Iterable<Element> elements = XMLUtils.selectElements(plugin.getDocument(), "/plugin/endpoint[@id='directory.param']");
            if (elements != null) {
                for (Element endpoint : elements) {
                    String entity = endpoint.getAttribute("entity");

                    if (!entity.equals("")) {
                        directoryMap.put(entity + "Parameter", endpoint.getAttribute("title"));
                    }
                }
            }
        }
    }

    @Override
    protected ActionForward unspecified(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        return directory(mapping, form);
    }

    public ActionForward directory(ActionMapping mapping, DynActionForm form) {
        form.setParam("directoryId", "default");
        setDirectoryList(form.getHttpRequest());

        return mapping.findForward(FORWARD_DEFAULT);
    }

    // параметры
    public ActionForward parameterList(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        ParamDAO paramDAO = new ParamDAO(con, form.getUserId());
        var request = form.getHttpRequest();

        setDirectoryList(request);
        SearchResult<Parameter> searchResult = new SearchResult<Parameter>(form);

        paramDAO.getParameterList(searchResult, getObjectType(form.getParam("directoryId")),
                CommonDAO.getLikePattern(form.getParam("filter"), "subs"), 0, null);

        request.setAttribute("parameterList", searchResult.getList());

        return mapping.findForward("parameterList");
    }

    public ActionForward parameterUseProcess(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        Integer paramId = Utils.parseInt(form.getParam("parameterId"));
        List<String> containProcess = new ArrayList<String>();
        Map<Integer, ProcessType> processTypeMap = ProcessTypeCache.getProcessTypeMap();

        for (int i = 0; i < processTypeMap.size(); i++) {
            ProcessType pType = (ProcessType) processTypeMap.values().toArray()[i];

            if (!pType.isUseParentProperties()) {
                List<Integer> parameters = pType.getProperties().getParameterIds();
                if (parameters.contains(paramId)) {
                    containProcess.add(pType.getTitle());
                }
            }
        }

        form.getResponse().setData("containProcess", containProcess);

        return data(con, mapping, form, "parameterUseProcess");
    }

    public ActionForward parameterGet(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        ParamDAO paramDAO = new ParamDAO(con, form.getUserId());

        Parameter parameter = paramDAO.getParameter(form.getId());
        if (parameter != null) {
            form.getResponse().setData("parameter", parameter);
        }

        var request = form.getHttpRequest();
        setDirectoryList(request);
        request.setAttribute("directoryTitle", directoryMap.get(form.getParam("directoryId")));

        return data(con, mapping, form, "parameterUpdate");
    }

    public ActionForward parameterUpdate(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        ParamDAO paramDAO = new ParamDAO(con, form.getUserId());

        Parameter parameter = new Parameter();
        parameter.setId(form.getId());
        parameter.setObject(getObjectType(form.getParam("directoryId")));
        parameter.setType(form.getParam("type"));

        if (form.getId() > 0) {
            parameter = paramDAO.getParameter(form.getId());
        }

        parameter.setTitle(form.getParam("title"));
        parameter.setOrder(Utils.parseInt(form.getParam("order")));
        parameter.setScript(form.getParam("script", ""));
        parameter.setConfig(form.getParam("config"));
        parameter.setComment(form.getParam("comment"));

        if (Parameter.TYPE_LIST.equals(parameter.getType()) || Parameter.TYPE_LISTCOUNT.equals(parameter.getType())
                || Parameter.TYPE_TREE.equals(parameter.getType())) {
            parameter.setValuesConfig(form.getParam("listValues"));
        }

        paramDAO.updateParameter(parameter);

        ParameterCache.flush(con);

        return status(con, form);
    }

    public ActionForward parameterDelete(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        new ParamDAO(con, form.getUserId()).deleteParameter(form.getId());

        ParameterCache.flush(con);

        return status(con, form);
    }

    // шаблоны названия
    public ActionForward patternTitleList(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        PatternDAO patternDAO = new PatternDAO(con);
        var request = form.getHttpRequest();

        String objectType = getObjectType(form.getParam("directoryId"));
        setDirectoryList(request);
        request.setAttribute("patternList", patternDAO.getPatternList(objectType));

        return mapping.findForward("patternList");
    }

    public ActionForward patternTitleGet(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        Pattern pattern = new PatternDAO(con).getPattern(form.getId());
        if (pattern != null) {
            form.getResponse().setData("pattern", pattern);
        }

        var request = form.getHttpRequest();
        setDirectoryList(request);
        request.setAttribute("directoryTitle", directoryMap.get(form.getParam("directoryId")));

        return data(con, mapping, form, "patternUpdate");
    }

    public ActionForward patternTitleUpdate(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        PatternDAO paramDAO = new PatternDAO(con);

        Pattern pattern = new Pattern();
        pattern.setId(form.getId());
        pattern.setObject(getObjectType(form.getParam("directoryId")));
        pattern.setTitle(form.getParam("title"));
        pattern.setPattern(form.getParam("pattern"));
        paramDAO.updatePattern(pattern);

        return status(con, form);
    }

    public ActionForward patternTitleDelete(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        new PatternDAO(con).deletePattern(form.getId());

        return status(con, form);
    }

    // группы параметров
    public ActionForward parameterGroupList(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        ParamGroupDAO paramGroupDAO = new ParamGroupDAO(con);

        var request = form.getHttpRequest();
        setDirectoryList(request);
        request.setAttribute("parameterList", paramGroupDAO.getParameterGroupList(form.getParam("directoryId")));

        return mapping.findForward("parameterGroupList");
    }

    //переписать "!" 
    public ActionForward parameterGroupGet(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        int id = form.getId();

        ParamDAO paramDAO = new ParamDAO(con, form.getUserId());
        ParamGroupDAO paramGroupDAO = new ParamGroupDAO(con);

        ParameterGroup parameterGroup = paramGroupDAO.getParameterGroup(id);
        if (parameterGroup != null) {
            parameterGroup.setParameterIds(paramGroupDAO.getParameterIdsForGroup(id));
            form.getResponse().setData("group", parameterGroup);
        }

        var request = form.getHttpRequest();
        setDirectoryList(request);
        request.setAttribute("parameterList", paramDAO.getParameterList(getObjectType(form.getParam("directoryId")), 0)); //!!!
        request.setAttribute("directoryTitle", directoryMap.get(form.getParam("directoryId")));

        return data(con, mapping, form, "parameterGroupUpdate");
    }

    public ActionForward parameterGroupUpdate(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        ParamGroupDAO paramGroupDAO = new ParamGroupDAO(con);

        ParameterGroup parameterGroup = new ParameterGroup();
        parameterGroup.setId(form.getId());
        parameterGroup.setObject(getObjectType(form.getParam("directoryId")));
        parameterGroup.setTitle(form.getParam("title"));
        parameterGroup.setParameterIds(form.getSelectedValues("param"));
        paramGroupDAO.updateParameterGroup(parameterGroup);

        ParameterCache.flush(con);

        return status(con, form);
    }

    public ActionForward parameterGroupDelete(ActionMapping mapping, DynActionForm form, Connection con) throws Exception {
        new ParamGroupDAO(con).deleteParameterGroup(form.getId());

        ParameterCache.flush(con);

        return status(con, form);
    }

    private void setDirectoryList(HttpServletRequest request) {
        if (directoryList == null) {
            directoryList = new ArrayList<ListItem>();
            for (String key : directoryMap.keySet()) {
                ListItem listItem = new ListItem();
                listItem.setKey(key);
                listItem.setTitle(directoryMap.get(key));
                directoryList.add(listItem);
            }
            Collections.sort(directoryList, new Comparator<ListItem>() {
                @Override
                public int compare(ListItem o1, ListItem o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });
        }
        request.setAttribute("directoryList", directoryList);
    }

    private String getObjectType(String directoryId) {
        String objectType = null;
        if (directoryId != null) {
            if (directoryId.startsWith("customer")) {
                objectType = Customer.OBJECT_TYPE;
            } else if (directoryId.startsWith("user")) {
                objectType = User.OBJECT_TYPE;
            } else if (directoryId.startsWith("common")) {
                objectType = "common";
            }
            else if (directoryId.startsWith("process")) {
                objectType = Process.OBJECT_TYPE;
            } else if (directoryId.startsWith("address")) {
                objectType = AddressHouse.OBJECT_TYPE;
            }
            // TODO: вариант для параметров плагинов
            else {
                PluginManager pluginManager = PluginManager.getInstance();
                for (Plugin plugin : pluginManager.getPluginList()) {
                    Iterable<Element> endpoints = XMLUtils.selectElements(plugin.getDocument(), "/plugin/endpoint[@id='directory.param']");

                    if (endpoints != null) {
                        for (Element endpoint : endpoints) {
                            String entity = endpoint.getAttribute("entity");

                            if (directoryId.startsWith(entity)) {
                                objectType = entity;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return objectType;
    }
}
