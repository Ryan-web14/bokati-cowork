package com.sni.bokaticowork.core.audit.mapper.decorator;


import com.sni.bokaticowork.core.audit.dto.response.UserSessionResponse;
import com.sni.bokaticowork.core.audit.mapper.interfaces.UserSessionMapper;
import com.sni.bokaticowork.core.audit.model.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public abstract class UserSessionMapperDecorator implements UserSessionMapper {

    @Autowired
    @Qualifier("delegate")
    private UserSessionMapper delegate;

    @Override
    public UserSessionResponse toDTO(UserSession obj){

        if(obj == null) {
            log.debug("UserSession is null");
            return null;}

        UserSessionResponse response = delegate.toDTO(obj);

        response.setSessionId(obj.getSessionId().toString());
        response.setEmail(obj.getUsers().getEmail());
        response.setRole(obj.getRoleSnapshot());

        if(!obj.isActive()){
            response.setStatus("Expired");
        }else{
            response.setStatus("Active");
        }

        return response;
    }


}
