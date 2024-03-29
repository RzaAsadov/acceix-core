/*
 * The MIT License
 *
 * Copyright 2022 Rza Asadov (rza at asadov dot me).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.acceix.frontend.core;



import java.io.BufferedReader;
import org.acceix.frontend.core.accounts.Accounts;
import org.acceix.frontend.helpers.RequestObject;
import org.acceix.frontend.helpers.ModuleHelper;
import org.acceix.frontend.web.commons.DataUtils;
import org.acceix.frontend.web.commons.FrontendSecurity;
import org.acceix.ndatabaseclient.exceptions.MachineDataException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.ParseException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.MultipartConfigElement;
import org.acceix.logger.NLog;
import org.acceix.logger.NLogBlock;
import org.acceix.logger.NLogger;
 
/**
 *
 * @author zrid
 */

public class Engine extends HttpServlet {


    public static  Map<String,Object> envs;
    public static List<ModuleHelper> modules;
    
    private Accounts authenticationManager = new  Accounts();   

    public static void setEnvs(Map<String, Object> envs) {
        Engine.envs = envs;
    }

    public static void setModules(List<ModuleHelper> t_modules) {
        modules = t_modules;
    }
    
    
    
        public RequestObject readRequestParameters(HttpServletRequest request) {
            
            

                RequestObject requestObject = new RequestObject();
                String module=null,action=null;
                
                Enumeration<String> requestEnum =  request.getParameterNames();
                
                Map<String,String> requestParams = new HashMap<>();
                
                boolean is_debug_activated = false;
                
                if (requestEnum.hasMoreElements()) { // We have params
                    
                        while (requestEnum.hasMoreElements()) {

                            String paramkey = requestEnum.nextElement();
                            
                            requestParams.put(paramkey, request.getParameter(paramkey));
                            
                            if (paramkey.equals("netondo_debug_mode_x999")) {
                                if (request.getParameter(paramkey).equals("activated")) {
                                    is_debug_activated = true;
                                }
                            }
                            
                            

                            switch (paramkey) {
                                case "module":
                                    module = request.getParameter(paramkey);
                                    break;
                                case "action":
                                    action = request.getParameter(paramkey);
                                    break;
                                default:
                                    requestObject.getParams().put(paramkey, request.getParameter(paramkey));
                                    break;
                            }

                        }
                        
                        if (is_debug_activated) {
                            
                                System.out.println("Input DEBUG");
                                
                                requestParams.forEach( (key,value) -> {
                                   
                                    System.out.println(key + " = " + value);
                                    
                                });
                            
                        }

                        
                        if (request.getParameter("module") == null) {
                            module = (String) "crud";
                        }   
                                                     
                        
                        if (module != null && action != null) {
                               requestObject.setModule(module);
                               requestObject.setAction(action);
                               
                               requestObject.setRequestType(RequestObject.REQUEST_DATA_TYPE_HTTP);
                               
                        } else {
                            requestObject.setRequestType(RequestObject.REQUEST_DATA_TYPE_NO_MANDATORY_KEYS);
                        }
                        

                } //else { // we have json

                    DataUtils dataUtils = new DataUtils();
                    
                    Object requestBodyJson = null;

                        
                        String requestBody = null;
                        try {
                            requestBody = dataUtils.readRequestBody(request.getInputStream());
                        } catch (IOException ex) {
                            Logger.getLogger(Engine.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        if (requestBody != null) {
                                                    
                            requestObject.setRequestBody(requestBody);


                                try { 
                                    
                                    if (requestBody.contains("netondo_debug_mode_x999")) {
                                        System.out.println(requestBody);
                                    }
                                                              
                                    
                                    if (requestBody.charAt(requestBody.length()-1)==0) {
                                        requestBody = requestBody.substring(0, requestBody.length()-1);
                                    }
                                    
                                    requestBodyJson = dataUtils.readJsonArrayFromString(requestBody);

                                    
                                } catch (IOException | org.json.simple.parser.ParseException ex) {
                                    NLogger.logger(NLogBlock.WEB, NLog.ERROR, "engine", "readJsonArrayFromString", "system", "Invalid JSON: " + requestBody);
                                }   

                        
                        }

                    
                        if (requestBodyJson != null) {

                                if (requestBodyJson instanceof List) {
                                    
                                    List jSONArray = (List) requestBodyJson;

                            
                                    Map<String,Object> inputData = dataUtils.jsonFormDataToMap(jSONArray,"name","value");
                                    
                                    if (inputData.get("module") == null) {
                                        module = (String) "crud";
                                    } else {
                                        module = (String)inputData.get("module");
                                    }
                                    action = (String)inputData.get("action");

                                    if (module != null && action != null) {

                                        List requestFiltered = new LinkedList();


                                        for (int i=0; i < jSONArray.size(); i++) {

                                                Object t = jSONArray.get(i);
                                                Map jSONObjectInArray = (Map)t;                                    

                                                String name = (String)jSONObjectInArray.get("name");

                                                if (!name.equals("mode")) requestFiltered.add(t);

                                        }



                                            requestObject.setRawInput(requestFiltered);                                    
                                            requestObject.setModule(module);
                                            requestObject.setAction(action);
                                            inputData.remove("module");
                                            inputData.remove("action");
                                            requestObject.getParams().putAll(inputData);
                                            requestObject.setRequestType(RequestObject.REQUEST_DATA_TYPE_JSON);
                                    }

                                } else {
                                        requestObject.setRequestType(RequestObject.REQUEST_DATA_TYPE_NO_MANDATORY_KEYS);
                                }
                        }


                
                return requestObject;

                
        }
    
    
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, ParseException, ClassNotFoundException, org.json.simple.parser.ParseException, SQLException, MachineDataException, IOException {

            //System.out.println("processRequest called #1");
            

                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
                response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
                response.setHeader("Expires", "0"); // Proxies.
                
                
                    /*
                        BufferedReader bufr = new BufferedReader(new InputStreamReader(request.getInputStream(),StandardCharsets.UTF_8));
                
                        String jsonSTR = "";
                        String inbuffer;

                        while (( inbuffer = bufr.readLine()) != null) {
                            jsonSTR = jsonSTR + inbuffer;
                        }

                        System.out.println(jsonSTR);
                   */
                
            
                long maxFileSize = Long.parseLong((String)envs.get("max_upload_size"));
                long maxRequestSize = Long.parseLong((String)envs.get("max_form_content_size"));
                int fileSizeThreshold = Integer.parseInt((String)envs.get("file_size_threshold"));
                    
                MultipartConfigElement multipartConfig = new MultipartConfigElement((String)envs.get("upload_tmp"), maxFileSize, maxRequestSize, fileSizeThreshold);            

                request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfig);
            
                String calledModule = "",licensekey=""; 
                
                

                RequestObject requestObject = readRequestParameters(request);
                
                if (requestObject==null) return;
                        
                
                   if ((boolean)this.envs.get("isAppCreateMode")) {
                       requestObject.setModule("setup");
                       requestObject.setAction("databasepage");
                   }

                                    
                    for (ModuleHelper webModule : modules) {
                        
                            if (webModule.getModuleName().equals(requestObject.getModule())) {
                                
                                    //System.out.println("Called module->" + requestObject.getModule() + ", action>" + requestObject.getAction());
                                
                                    ModuleHelper moduleToCall = webModule.getInstance();
                                    moduleToCall.construct();
                                    moduleToCall.setRequestObject(requestObject);
                                    moduleToCall.setupWebModule(response, request);
                                    moduleToCall.setGobalEnv(this.envs);
                                    moduleToCall.init();
                                    moduleToCall.processModule();
                                    return;
                                    
                            }
                            
                    } 
                    
                    // The last check
                    if ( (calledModule.length()==0) ) {
                     
                        authenticationManager.setRequestObject(requestObject);
                        authenticationManager.setupWebModule(response, request);
                        authenticationManager.setGobalEnv(this.envs);
                        authenticationManager.init();
                        authenticationManager.gotoMainPageOrSigninAgain();

                    }


    }

 
    
    public static boolean accessCheck(HttpServletRequest request,HttpServletResponse response,List<String> roles,String rolename) {

        if (isRoleEnabled(roles, rolename)) {
            try {
                FrontendSecurity.securityCaseHandler(request, response);
            } catch (Exception ex) {
                Logger.getLogger(Engine.class.getName()).log(Level.SEVERE, null, ex);
            }
            return false;
        } else {
            return true;
        }
    }
    
    public static boolean isRoleEnabled(List<String> roles,String rolename) {
        
        
      if (rolename.charAt(rolename.length()-1)=='*') {
            return roles.stream().anyMatch(role -> (role.startsWith(rolename.substring(0, rolename.length()-1))));          
        } else {
            return roles.stream().anyMatch(role -> (role.equals(rolename)));
        }
    }       
    

    
   
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException {
        try {
            
            request.setCharacterEncoding("UTF-8");              

            processRequest(request, response);
        } catch (ParseException | ClassNotFoundException | org.json.simple.parser.ParseException | SQLException ex) {
            Logger.getLogger(Engine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MachineDataException ex) {
            Logger.getLogger(Engine.class.getName()).log(Level.SEVERE, null, ex.getExternalException());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            
            request.setCharacterEncoding("UTF-8");            
            
            processRequest(request, response);
        } catch (ParseException | ClassNotFoundException | org.json.simple.parser.ParseException | SQLException ex) {
            Logger.getLogger(Engine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MachineDataException ex) {
            Logger.getLogger(Engine.class.getName()).log(Level.SEVERE, null, ex.getExternalException());
        }
    }


}
