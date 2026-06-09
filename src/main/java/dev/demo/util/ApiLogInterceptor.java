package dev.demo.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Log4j2
@Component
public class ApiLogInterceptor implements HandlerInterceptor
{

	private final Map<UUID, Long> timeMap = new ConcurrentHashMap<>();

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
	{
		try
		{
			var uuid = UUID.randomUUID();
			request.setAttribute("api_log_uuid", uuid);

			var begin = String.format("%-20s: %s %s", //
					String.format("[Request Start (%s)]", uuid.toString()), //
					request.getMethod(), //
					url(request));

			log(begin);

			timeMap.put(uuid, System.currentTimeMillis());
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}

		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e)
	{
		try
		{
			Object uuid = request.getAttribute("api_log_uuid");
			Object duration = "";
			if (uuid instanceof UUID _uuid)
			{
				var start = timeMap.get(_uuid);
				if (start != null)
				{
					duration = "" + (System.currentTimeMillis() - start) / 1000f;
					timeMap.remove(_uuid);
				}
			}


			var route = String.format("%-20s: %s %s (time: %ss) %s", //
					String.format("[Request End (%s)]", uuid.toString()), //
					request.getMethod(), //
					response.getStatus(), //
					duration, //
					url(request));

			if (e == null)
				log(route);
			else
				log.error(route, e);
		}
		catch (Exception ee)
		{
			log.error(ee.getMessage(), ee);
		}
	}

	private void log(String message)
	{
		log.debug(message);
	}

	private String url(HttpServletRequest request)
	{
		var uri = new StringBuilder();
		uri.append(request.getRequestURI());

		if (isNotEmpty(request.getQueryString()))
		{
			uri.append("?");
			uri.append(request.getQueryString());
		}

		return uri.toString();
	}
}
