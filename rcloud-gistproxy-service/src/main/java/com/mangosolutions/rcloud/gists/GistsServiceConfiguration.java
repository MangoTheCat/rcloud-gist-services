/*******************************************************************************
* Copyright (c) 2017 AT&T Intellectual Property, [http://www.att.com]
*
* SPDX-License-Identifier:   MIT
*
*******************************************************************************/
package com.mangosolutions.rcloud.gists;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import com.mangosolutions.rcloud.gists.filters.HeaderUrlRewritingFilter;
import com.mangosolutions.rcloud.gists.filters.JsonContentUrlRewritingFilter;
import com.netflix.zuul.ZuulFilter;

/**
 * Main Spring configuration
 *
 */
@Configuration
public class GistsServiceConfiguration {

	@Bean
	public ZuulFilter getUrlRewritingFilter() {
		return new HeaderUrlRewritingFilter(10);
	}

	@Bean
	public ZuulFilter getJsonContentUrlRewritingFilter() {
		return new JsonContentUrlRewritingFilter(20);
	}
	
	@Bean
	public CommonsRequestLoggingFilter requestLoggingFilter() {
	    CommonsRequestLoggingFilter crlf = new CommonsRequestLoggingFilter();
	    crlf.setIncludeClientInfo(true);
	    crlf.setIncludeQueryString(true);
	    crlf.setIncludePayload(true);
	    return crlf;
	}

}
