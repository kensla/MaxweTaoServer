package org.maxwe.tao.server.controller.user.agent;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.taobao.api.ApiException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.maxwe.tao.server.common.cache.SessionContext;
import org.maxwe.tao.server.common.response.IResultSet;
import org.maxwe.tao.server.common.response.ResultSet;
import org.maxwe.tao.server.common.sms.SMSManager;
import org.maxwe.tao.server.common.utils.CellPhoneUtils;
import org.maxwe.tao.server.common.utils.Code;
import org.maxwe.tao.server.common.utils.IPUtils;
import org.maxwe.tao.server.interceptor.TokenInterceptor;
import org.maxwe.tao.server.service.user.CSEntity;
import org.maxwe.tao.server.service.user.agent.AgentEntity;
import org.maxwe.tao.server.service.user.agent.AgentServices;
import org.maxwe.tao.server.service.user.agent.IAgentServices;

import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by Pengwei Ding on 2016-12-25 14:57.
 * Email: www.dingpengwei@foxmail.com www.dingpegnwei@gmail.com
 * Description: @TODO
 */
public class AgentController extends Controller implements IAgentController {
    private Log logger = LogFactory.getLog(AgentController.class);
    private IAgentServices iAgentServices = new AgentServices();

    @Override
    public void exist() {
        String params = this.getPara("p");
        VAgentEntity requestVAgentEntity = JSON.parseObject(params, VAgentEntity.class);
        IResultSet iResultSet = new ResultSet();
        if (requestVAgentEntity == null) {
            iResultSet.setCode(IResultSet.ResultCode.RC_PARAMS_BAD.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_PARAMETERS_BAD);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone")));
            return;
        }
        //重复检测
        AgentEntity existAgent = iAgentServices.existAgent(requestVAgentEntity);
        String password = null;
        if (requestVAgentEntity.getType() == 1) {
            password = existAgent.getPassword1();
        } else if (requestVAgentEntity.getType() == 2) {
            password = existAgent.getPassword2();
        }
        if (existAgent != null && password != null) {
            iResultSet.setCode(IResultSet.ResultCode.RC_PARAMS_REPEAT.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_CANNOT_REPEAT);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone")));
            return;
        }

        iResultSet.setCode(IResultSet.ResultCode.RC_SUCCESS.getCode());
        iResultSet.setData(requestVAgentEntity);
        iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_OK);
        renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone")));
    }

    @Override
    public void smsCode() {
        String params = this.getPara("p");
        this.logger.info("smsCode = " + params);
        VAgentEntity requestVAgentEntity = JSON.parseObject(params, VAgentEntity.class);
        IResultSet iResultSet = new ResultSet();
        if (requestVAgentEntity == null || !CellPhoneUtils.isCellphone(requestVAgentEntity.getCellphone())) {
            iResultSet.setCode(IResultSet.ResultCode.RC_PARAMS_BAD.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_PARAMETERS_BAD);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone")));
            return;
        }
        if (SMSManager.isCacheAddress(IPUtils.getIpAddress(this.getRequest()))) {
            iResultSet.setCode(IResultSet.ResultCode.RC_TO_MANY.getCode());
            iResultSet.setData(requestVAgentEntity.getCellphone());
            iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_OK);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(AgentEntity.class, "t")));
            return;
        }

        try {
            SMSManager.sendSMS(requestVAgentEntity.getCellphone());
        } catch (ApiException e) {
            e.printStackTrace();
        }
        iResultSet.setCode(IResultSet.ResultCode.RC_SUCCESS.getCode());
        iResultSet.setData(requestVAgentEntity);
        iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_OK);
        renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone")));
    }

    @Override
    public void create() {
        String params = this.getPara("p");
        VAgentEntity requestVAgentEntity = JSON.parseObject(params, VAgentEntity.class);
        IResultSet iResultSet = new ResultSet();
        //参数检测
        if (requestVAgentEntity == null || !requestVAgentEntity.checkCreateParams()) {
            iResultSet.setCode(IResultSet.ResultCode.RC_PARAMS_BAD.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_PARAMETERS_BAD);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone", "password")));
            return;
        }
        // 验证码检测
        if (!StringUtils.equals(requestVAgentEntity.getCellPhoneCode(), SMSManager.getCellphoneCode(requestVAgentEntity.getCellphone()))) {
            iResultSet.setCode(IResultSet.ResultCode.RC_PARAMS_BAD.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_PARAMETERS_BAD);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone", "password")));
            return;
        }
        //重复检测 同一种类型下的同一个电话号码算是重复
        AgentEntity existAgent = iAgentServices.existAgent(requestVAgentEntity);
        if (existAgent != null) {
            // 电话号码重复
            String password = null;
            if (requestVAgentEntity.getType() == 1) {
                password = existAgent.getPassword1();
            } else if (requestVAgentEntity.getType() == 2) {
                password = existAgent.getPassword2();
            }
            if (password != null) {
                // 电话号码注册类型重复
                iResultSet.setCode(IResultSet.ResultCode.RC_PARAMS_REPEAT.getCode());
                iResultSet.setData(requestVAgentEntity);
                iResultSet.setMessage(IResultSet.ResultMessage.RM_CANNOT_REPEAT);
                renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(AgentEntity.class, "cellphone", "password")));
                return;
            }
        }

        AgentEntity agent;
        if (existAgent == null) {
            //直接创建
            requestVAgentEntity.setAgentId(UUID.randomUUID().toString());
            requestVAgentEntity.setPassword1(requestVAgentEntity.getPassword());
            requestVAgentEntity.setPassword2(requestVAgentEntity.getPassword());
            agent = iAgentServices.createAgent(requestVAgentEntity);
        } else {
            if (requestVAgentEntity.getType() == 1) {
                existAgent.setPassword1(requestVAgentEntity.getPassword());
            } else if (requestVAgentEntity.getType() == 2) {
                existAgent.setPassword2(requestVAgentEntity.getPassword());
            }
            agent = iAgentServices.updateAgentType(existAgent);
        }

        if (agent == null) {
            iResultSet.setCode(IResultSet.ResultCode.RC_SEVER_ERROR.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_ERROR);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(AgentEntity.class, "cellphone", "password")));
            return;
        }

        CSEntity agentCS = new CSEntity(agent.getAgentId(), agent.getCellphone(), Code.getToken(agent.getCellphone(), requestVAgentEntity.getPassword()), requestVAgentEntity.getType());
        SessionContext.addCSEntity(agentCS);

        //创建
        iResultSet.setCode(IResultSet.ResultCode.RC_SUCCESS.getCode());
        iResultSet.setData(agentCS.getToken());
        iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_OK);
        renderJson(JSON.toJSONString(iResultSet));
    }

    @Override
    public void lost() {
        String params = this.getPara("p");
        this.logger.info("lostPassword = " + params);
        VAgentEntity requestVAgentEntity = JSON.parseObject(params, VAgentEntity.class);
        IResultSet iResultSet = new ResultSet();
        //参数检测
        if (requestVAgentEntity == null || !requestVAgentEntity.checkCreateParams()) {
            iResultSet.setCode(IResultSet.ResultCode.RC_PARAMS_BAD.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_PARAMETERS_BAD);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone", "password")));
            return;
        }
        // 注册检测
        AgentEntity existAgent = iAgentServices.existAgent(requestVAgentEntity);
        if (existAgent == null) {
            // 电话号码没有注册
            iResultSet.setCode(IResultSet.ResultCode.RC_ACCESS_BAD_2.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_PARAMETERS_BAD);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone", "password")));
            return;
        } else {
            // 电话号码注册，但是该类型下的没注册
            String password = null;
            if (requestVAgentEntity.getType() == 1) {
                password = existAgent.getPassword1();
            } else if (requestVAgentEntity.getType() == 2) {
                password = existAgent.getPassword2();
            }
            if (password == null) {
                iResultSet.setCode(IResultSet.ResultCode.RC_ACCESS_BAD_2.getCode());
                iResultSet.setData(requestVAgentEntity);
                iResultSet.setMessage(IResultSet.ResultMessage.RM_PARAMETERS_BAD);
                renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone", "password")));
                return;
            }
        }

        // 验证码检测
        if (!StringUtils.equals(requestVAgentEntity.getCellPhoneCode(), SMSManager.getCellphoneCode(requestVAgentEntity.getCellphone()))) {
            iResultSet.setCode(IResultSet.ResultCode.RC_PARAMS_BAD.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_PARAMETERS_BAD);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone", "password")));
            return;
        }
        if (requestVAgentEntity.getType() == 1) {
            existAgent.setPassword1(requestVAgentEntity.getPassword());
        } else if (requestVAgentEntity.getType() == 2) {
            existAgent.setPassword2(requestVAgentEntity.getPassword());
        }
        existAgent.setType(requestVAgentEntity.getType());
        AgentEntity agent = iAgentServices.updateAgentPassword(existAgent);
        if (agent == null) {
            iResultSet.setCode(IResultSet.ResultCode.RC_SEVER_ERROR.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_LOGIN_FAIL);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone", "password")));
            return;
        }

        CSEntity agentCS = new CSEntity(agent.getAgentId(), agent.getCellphone(), Code.getToken(agent.getCellphone(), requestVAgentEntity.getPassword()), requestVAgentEntity.getType());
        SessionContext.addCSEntity(agentCS);
        //创建
        iResultSet.setCode(IResultSet.ResultCode.RC_SUCCESS.getCode());
        iResultSet.setData(agentCS.getToken());
        iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_OK);
        renderJson(JSON.toJSONString(iResultSet));
    }

    @Override
    public void login() {
        String params = this.getPara("p");
        this.logger.info("login = " + params);
        VAgentEntity requestVAgentEntity = JSON.parseObject(params, VAgentEntity.class);
        IResultSet iResultSet = new ResultSet();
        if (requestVAgentEntity == null || !requestVAgentEntity.checkLoginParams()) {
            iResultSet.setCode(IResultSet.ResultCode.RC_PARAMS_BAD.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_PARAMETERS_BAD);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone", "password")));
            return;
        }

        //查找
        requestVAgentEntity.setPassword1(requestVAgentEntity.getPassword());
        requestVAgentEntity.setPassword2(requestVAgentEntity.getPassword());
        AgentEntity agentEntity = iAgentServices.retrieveAgent(requestVAgentEntity);
        if (agentEntity == null) {
            iResultSet.setCode(IResultSet.ResultCode.RC_ACCESS_BAD.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_LOGIN_FAIL);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "cellphone", "password")));
            return;
        }

        CSEntity agentCS = new CSEntity(agentEntity.getAgentId(), agentEntity.getCellphone(), Code.getToken(agentEntity.getCellphone(), requestVAgentEntity.getPassword()), requestVAgentEntity.getType());
        SessionContext.addCSEntity(agentCS);
        iResultSet.setCode(IResultSet.ResultCode.RC_SUCCESS.getCode());
        iResultSet.setData(agentCS.getToken());
        iResultSet.setMessage(IResultSet.ResultMessage.RM_LOGIN_SUCCESS);
        renderJson(JSON.toJSONString(iResultSet));
    }

    @Override
    @Before(TokenInterceptor.class)
    public void logout() {
        String params = this.getPara("p");
        this.logger.info("login = " + params);
        VAgentEntity requestVAgentEntity = JSON.parseObject(params, VAgentEntity.class);
        IResultSet iResultSet = new ResultSet();
        if (requestVAgentEntity == null || StringUtils.isEmpty(requestVAgentEntity.getT())) {
            iResultSet.setCode(IResultSet.ResultCode.RC_PARAMS_BAD.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_PARAMETERS_BAD);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "t")));
            return;
        }
        CSEntity csEntity = new CSEntity(null, requestVAgentEntity.getCellphone(), requestVAgentEntity.getT(), requestVAgentEntity.getType());
        SessionContext.delCSEntity(csEntity);
        iResultSet.setCode(IResultSet.ResultCode.RC_SUCCESS.getCode());
        iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_OK);
        renderJson(JSON.toJSONString(iResultSet));
    }

    @Override
    @Before(TokenInterceptor.class)
    public void password() {
        String params = this.getPara("p");
        VAgentEntity requestVAgentEntity = JSON.parseObject(params, VAgentEntity.class);
        IResultSet iResultSet = new ResultSet();
        if (requestVAgentEntity == null || !requestVAgentEntity.checkModifyPasswordParams()) {
            iResultSet.setCode(IResultSet.ResultCode.RC_PARAMS_BAD.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_PARAMETERS_BAD);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "t", "orderPassword", "newPassword")));
            return;
        }

        CSEntity csEntity = new CSEntity(null, requestVAgentEntity.getCellphone(), requestVAgentEntity.getT(), requestVAgentEntity.getType());
        CSEntity existCSEntity = SessionContext.getCSEntity(csEntity);
        AgentEntity existAgentEntity = iAgentServices.retrieveAgentById(existCSEntity.getAgentId());
        if (existAgentEntity == null) {
            iResultSet.setCode(IResultSet.ResultCode.RC_SEVER_ERROR.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_ERROR);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "t", "orderPassword", "newPassword")));
            return;
        }

        if (requestVAgentEntity.getType() == 1) {
            if (!existAgentEntity.getPassword1().equals(requestVAgentEntity.getOrdPassword())) {
                iResultSet.setCode(IResultSet.ResultCode.RC_ACCESS_BAD.getCode());
                iResultSet.setData(requestVAgentEntity);
                iResultSet.setMessage(IResultSet.ResultMessage.RM_ACCESS_BAD);
                renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "t", "orderPassword", "newPassword")));
                return;
            }
            existAgentEntity.setPassword1(requestVAgentEntity.getNewPassword());
        } else if (requestVAgentEntity.getType() == 2) {
            if (!existAgentEntity.getPassword2().equals(requestVAgentEntity.getOrdPassword())) {
                iResultSet.setCode(IResultSet.ResultCode.RC_ACCESS_BAD.getCode());
                iResultSet.setData(requestVAgentEntity);
                iResultSet.setMessage(IResultSet.ResultMessage.RM_ACCESS_BAD);
                renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "t", "orderPassword", "newPassword")));
                return;
            }
            existAgentEntity.setPassword2(requestVAgentEntity.getNewPassword());
        }
        existAgentEntity.setType(requestVAgentEntity.getType());
        AgentEntity updateAgentEntity = iAgentServices.updateAgentPassword(existAgentEntity);
        if (updateAgentEntity == null) {
            iResultSet.setCode(IResultSet.ResultCode.RC_SEVER_ERROR.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_ERROR);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "t", "orderPassword", "newPassword")));
            return;
        }

        CSEntity agentCS = new CSEntity(updateAgentEntity.getAgentId(), updateAgentEntity.getCellphone(), Code.getToken(updateAgentEntity.getCellphone(), requestVAgentEntity.getNewPassword()), requestVAgentEntity.getType());
        SessionContext.addCSEntity(agentCS);
        iResultSet.setCode(IResultSet.ResultCode.RC_SUCCESS.getCode());
        iResultSet.setData(agentCS.getToken());
        iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_OK);
        renderJson(JSON.toJSONString(iResultSet));
    }

    @Override
    @Before(TokenInterceptor.class)
    public void retrieveAgent() {
        String params = this.getPara("p");
        VAgentEntity requestVAgentEntity = JSON.parseObject(params, VAgentEntity.class);
        IResultSet iResultSet = new ResultSet();
        CSEntity agentCS = new CSEntity(null, requestVAgentEntity.getCellphone(), requestVAgentEntity.getT(), requestVAgentEntity.getType());
        AgentEntity agentEntity = iAgentServices.retrieveAgentById(SessionContext.getCSEntity(agentCS).getAgentId());
        VAgentEntity result = new VAgentEntity(agentEntity);
        iResultSet.setCode(IResultSet.ResultCode.RC_SUCCESS.getCode());
        iResultSet.setData(result);
        iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_OK);
        renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "name","code","level","haveCodes","spendCodes","leftCodes")));
    }

    @Override
    @Before(TokenInterceptor.class)
    public void retrieveSubAgents() {
        String params = this.getPara("p");
        VAgentEntity requestVAgentEntity = JSON.parseObject(params, VAgentEntity.class);
        IResultSet iResultSet = new ResultSet();
        CSEntity agentCS = new CSEntity(null, requestVAgentEntity.getCellphone(), requestVAgentEntity.getT(), requestVAgentEntity.getType());
        CSEntity csEntity = SessionContext.getCSEntity(agentCS);
        LinkedList<AgentEntity> agentEntities = iAgentServices.retrieveAgentByPId(csEntity.getAgentId());
        if (agentEntities == null) {
            iResultSet.setCode(IResultSet.ResultCode.RC_SEVER_ERROR.getCode());
            iResultSet.setData(requestVAgentEntity);
            iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_ERROR);
            renderJson(JSON.toJSONString(iResultSet, new SimplePropertyPreFilter(VAgentEntity.class, "t")));
            return;
        }
        LinkedList<VAgentEntity> responseVAgentEntities = new LinkedList<>();
        for (AgentEntity agentEntity : agentEntities) {
            responseVAgentEntities.add(new VAgentEntity(agentEntity));
        }
        if (responseVAgentEntities.size() == 0) {
            iResultSet.setCode(IResultSet.ResultCode.RC_SUCCESS_EMPTY.getCode());
        } else {
            iResultSet.setCode(IResultSet.ResultCode.RC_SUCCESS.getCode());
        }
        iResultSet.setData(responseVAgentEntities);
        iResultSet.setMessage(IResultSet.ResultMessage.RM_SERVER_OK);
        renderJson(JSON.toJSONString(iResultSet));
    }

    @Override
    @Before(TokenInterceptor.class)
    public void retrieveAgentByCellphone() {

    }

    @Override
    @Before(TokenInterceptor.class)
    public void grantToCellphone() {

    }

    @Override
    @Before(TokenInterceptor.class)
    public void tradeCodesToCellphone() {

    }
}
