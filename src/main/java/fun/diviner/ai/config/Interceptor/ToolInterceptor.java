package fun.diviner.ai.config.Interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import fun.diviner.ai.mapper.ToolMapper;
import fun.diviner.ai.service.UtilService;
import fun.diviner.ai.entity.database.Tool;
import fun.diviner.ai.entity.Trade;
import fun.diviner.ai.entity.Pay;
import fun.diviner.ai.entity.PayType;
import fun.diviner.ai.entity.Special;
import fun.diviner.ai.util.response.ExceptionResponse;
import fun.diviner.ai.util.response.ExceptionResponseCode;

@Component
public class ToolInterceptor implements HandlerInterceptor {
    @Autowired
    private ToolMapper tool;
    @Autowired
    private UtilService util;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String[] paths = request.getServletPath().split("/");
        String alias = String.join("_", Arrays.copyOfRange(paths, 2, paths.length));
        LambdaQueryWrapper<Tool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tool::getAlias, alias);
        Tool tool = this.tool.selectOne(wrapper);
        if (tool == null) {
            throw new ExceptionResponse(ExceptionResponseCode.PARAMETER_ERROR, "工具不存在");
        } else if (this.util.getCoreContent(Special.CoreName.openTool).equals("0")) {
            throw new ExceptionResponse(ExceptionResponseCode.RUN_ERROR, "工具因维护暂时关闭");
        }

        Trade trade = new Trade();
        trade.setId(tool.getId());
        Pay pay = this.util.pay(PayType.BALANCE, tool.getPrice());
        if (!pay.isState()) {
            throw new ExceptionResponse(ExceptionResponseCode.USER_ERROR, "余额不足");
        }
        trade.setPay(pay);
        this.util.setAttribute(Special.AttributeName.trade, trade);
        return true;
    }
}