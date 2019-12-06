package com.controller;

import com.haier.cxxt.marketFAndSalesP.entity.FactoryInventEntity;
import com.haier.cxxt.marketFAndSalesP.entity.QueryTerm;
import com.haier.cxxt.marketFAndSalesP.exceptions.EcServiceException;
import com.haier.cxxt.service.FactoryInventService;
import com.haier.cxxt.utils.DownloadUtil;
import com.haier.cxxt.utils.HttpJsonResult;
import com.haier.cxxt.utils.ret.RetResponse;
import com.haier.cxxt.utils.ret.RetResult;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 工厂库存表Controller
 */
@Controller
@RequestMapping(value = "cxxtweb/factoryinvent")
public class FactoryInventController {

    private static String INDEX_CY = "cy";// 产业
    private static String INDEX_CPZ = "cpz";// 产品组
    private static String INDEX_GC = "gc";//工厂
    //    自动注入工厂库存service
    @Autowired
    private FactoryInventService factoryInventService;

    private static final Logger logger = LoggerFactory.getLogger(FactoryInventController.class);

    /**
     * 获取数据列表分页
     *
     * @param request
     * @param response
     * @param factoryInventEntity
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/getFactoryInventList", method = RequestMethod.GET)
    public HttpJsonResult getClientIList(HttpServletRequest request, HttpServletResponse response, FactoryInventEntity factoryInventEntity) {
        logger.info("执行工厂库存表查询数据--cxxtweb/factoryinvent/getFactoryInventList");
        HttpJsonResult jsonResult = new HttpJsonResult<>();
        HashMap<String, Object> params = new HashMap<>();
        try {
            if (factoryInventEntity.getPageStart() != null)
                params.put("pageStart", (factoryInventEntity.getPageStart() - 1) * factoryInventEntity.getPageEnd() + 1);
            if (factoryInventEntity.getPageEnd() != null)
                params.put("pageEnd", factoryInventEntity.getPageStart() * factoryInventEntity.getPageEnd());

            params.put("sapfactoryname", factoryInventEntity.getSapfactoryname());
            params.put("newplc1ode", factoryInventEntity.getNewplc1ode());
            params.put("subplname", factoryInventEntity.getSubplname());
            params.put("materialcode", factoryInventEntity.getMaterialcode());
            params.put("wlcode", factoryInventEntity.getWlcode());
            params.put("cpflag", factoryInventEntity.getCpflag());
            params.put("materialdesc", factoryInventEntity.getMaterialdesc());
            params.put("producttype", factoryInventEntity.getProducttype());
            params.put("bxflag", factoryInventEntity.getBxflag());
            int count = factoryInventService.getFactoryListCount(params);
            if (count > 0) {
                jsonResult.setData(factoryInventService.getFactoryList(params));
            }
            jsonResult.setTotal(count);
            jsonResult.setCode("0");
        } catch (Exception e) {
            jsonResult.setMessage("服务异常!");
        }
        return jsonResult;
    }

    /**
     * 获取基本数据-页面下拉框数据
     *
     * @param request
     * @param response
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/getQueryData", method = RequestMethod.GET)
    public HttpJsonResult getQueryList(HttpServletRequest request, HttpServletResponse response, FactoryInventEntity factoryInventEntity) {
        logger.info("执行工厂库存存初始化筛选条件 --cxxtweb/factoryinvent/getQueryData");
        HttpJsonResult jsonResult = new HttpJsonResult<>();
        HashMap<String, Object> params = new HashMap<>();
        HashMap<String, Object> map = new HashMap<>();
        try {
            //查询下拉列表基础数据
            List<QueryTerm> queryTerms = factoryInventService.getQueryTerm(params);
            List<QueryTerm> newPlCodeList = new ArrayList<QueryTerm>();
            List<QueryTerm> subPlNameList = new ArrayList<QueryTerm>();
            List<QueryTerm> factoryList = new ArrayList<QueryTerm>();
            List<FactoryInventEntity> cpflag = new ArrayList<FactoryInventEntity>();
            //遍历查询到的下拉列表数据，按类别拆分
            if (queryTerms.size() > 0) {
                for (QueryTerm q : queryTerms) {
                    if (INDEX_GC.equals(q.getType())) {//工厂
                        factoryList.add(q);
                    } else if (INDEX_CY.equals(q.getType())) {//产业
                        newPlCodeList.add(q);
                    } else if (INDEX_CPZ.equals(q.getType())) {//产品组
                        subPlNameList.add(q);
                    }
                }
            }
            //获取所有的成品标识
            cpflag = factoryInventService.getCpFlag(params);
            map.put("newPlCodeList", newPlCodeList);
            map.put("subPlNameList", subPlNameList);
            map.put("factoryList", factoryList);
            map.put("cpflaglist", cpflag);
            jsonResult.setData(map);
        } catch (Exception e) {
            e.printStackTrace();
            jsonResult.setMessage(EcServiceException.SERVER_EXCEPTION);
        }
        return jsonResult;

    }

    /**
     * 导出Excel - V2
     *
     * @param request
     * @param response
     * @param factoryInventEntity
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/getFactoryExport", method = RequestMethod.GET)
    public HttpJsonResult exportExcel(HttpServletRequest request, HttpServletResponse response, FactoryInventEntity factoryInventEntity) {
        logger.info("执行工厂库存明细数据导出 **********************------------");
        HashMap<String, Object> params = new HashMap<>();
        List<FactoryInventEntity> factoryInventEntityList = new ArrayList<FactoryInventEntity>();
        params.put("sapfactoryname", factoryInventEntity.getSapfactoryname());
        params.put("newplc1ode", factoryInventEntity.getNewplc1ode());
        params.put("subplname", factoryInventEntity.getSubplname());
        params.put("materialcode", factoryInventEntity.getMaterialcode());
        params.put("wlcode", factoryInventEntity.getWlcode());
        params.put("cpflag", factoryInventEntity.getCpflag());
        params.put("materialdesc", factoryInventEntity.getMaterialdesc());
        params.put("producttype", factoryInventEntity.getProducttype());
        params.put("bxflag", factoryInventEntity.getBxflag());
        HttpJsonResult jsonResult = new HttpJsonResult<>();
        SimpleDateFormat dfs = new SimpleDateFormat("yyyy_MM_dd");
        String fileName = "工厂库存表_" + dfs.format(new Date()) + ".xlsx";
        try {
            int count = factoryInventService.getFactoryListCount(params);
            if (count == 0) {
                // 下载
                DownloadUtil.download(response, "factoryInventExport.xlsx", request);
                jsonResult.setMessage("无数据！");
                return jsonResult;
            }
            factoryInventEntityList = factoryInventService.getFactoryList(params); //获取数据
            File exportFile = DownloadUtil.createNewFile("factoryInventExport.xlsx", fileName);
            InputStream is = null;
            /*定义工作簿*/
            XSSFWorkbook workbook = null;
            XSSFSheet sheet = null;
            try {
                is = new FileInputStream(exportFile);
                workbook = new XSSFWorkbook(is);// 创建个workbook，
                // 获取第一个sheet表
                sheet = workbook.getSheetAt(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (sheet != null) {
                try {
                    XSSFCell cell = null;
                    XSSFRow row = sheet.getRow(0);
                    for (int i = 0; i < factoryInventEntityList.size(); i++) {
                        row = sheet.createRow(i + 2); // 从第3行开始
                        int j = 0;//数据坐标
                        createRowAndCell(factoryInventEntityList.get(i).getNewplc1ode(), row, cell, j++);//产业名称
                        createRowAndCell(factoryInventEntityList.get(i).getSubplname(), row, cell, j++);//产品组名称
                        createRowAndCell(factoryInventEntityList.get(i).getSapfactoryname(), row, cell, j++);//工厂小薇
                        createRowAndCell(factoryInventEntityList.get(i).getWlcode(), row, cell, j++);//库存地点
                        createRowAndCell(factoryInventEntityList.get(i).getMaterialcode(), row, cell, j++);//型号编码
                        createRowAndCell(factoryInventEntityList.get(i).getMaterialdesc(), row, cell, j++);//型号名称
                        createRowAndCell(factoryInventEntityList.get(i).getProducttype(), row, cell, j++);//内销/外销
                        createRowAndCell(factoryInventEntityList.get(i).getCpflag(), row, cell, j++);//成品标识
                        createRowAndCell(factoryInventEntityList.get(i).getBxflag(), row, cell, j++);//是否包销
                        createRowAndCell(factoryInventEntityList.get(i).getIbprice(), row, cell, j++);//标准供价
                        createRowAndCell(factoryInventEntityList.get(i).getSumk(), row, cell, j++);//总库存数量
                        createRowAndCell(factoryInventEntityList.get(i).getSumpr(), row, cell, j++);//总库存价格
                        createRowAndCell(factoryInventEntityList.get(i).getD03(), row, cell, j++);//0-3天库存数量
                        createRowAndCell(factoryInventEntityList.get(i).getD03ibprice(), row, cell, j++);//0-3天库存价格
                        createRowAndCell(factoryInventEntityList.get(i).getD47(), row, cell, j++); //4-7天库存数
                        createRowAndCell(factoryInventEntityList.get(i).getD47ibprice(), row, cell, j++); //4-7天库存价格
                        createRowAndCell(factoryInventEntityList.get(i).getD814(), row, cell, j++); //8-14天库存数
                        createRowAndCell(factoryInventEntityList.get(i).getD814ibprice(), row, cell, j++); //8-14天库存价格
                        createRowAndCell(factoryInventEntityList.get(i).getD1530(), row, cell, j++); //15-30天库存数
                        createRowAndCell(factoryInventEntityList.get(i).getD1530ibprice(), row, cell, j++); //15-30天库存价格
                        createRowAndCell(factoryInventEntityList.get(i).getD3160(), row, cell, j++);//31-60天库存数
                        createRowAndCell(factoryInventEntityList.get(i).getD3160ibprice(), row, cell, j++);//31-60天库存价格
                        createRowAndCell(factoryInventEntityList.get(i).getD6190(), row, cell, j++);//61-90天库存数
                        createRowAndCell(factoryInventEntityList.get(i).getD6190ibprice(), row, cell, j++); //61-90天库存价格
                        createRowAndCell(factoryInventEntityList.get(i).getD91180(), row, cell, j++); //91-180天库存数
                        createRowAndCell(factoryInventEntityList.get(i).getD91180ibprice(), row, cell, j++); //91-180天库存价格
                        createRowAndCell(factoryInventEntityList.get(i).getD180plus(), row, cell, j++);//大于180天库存数
                        createRowAndCell(factoryInventEntityList.get(i).getD180plusibprice(), row, cell, j++);//大于180天库存价格
                    }
                    DownloadUtil.export(response, workbook, fileName, request);

                } catch (Exception e1) {
                    e1.printStackTrace();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            jsonResult.setMessage(EcServiceException.SERVER_EXCEPTION);
        }
        return jsonResult;
    }

    /**
     * 根据当前row行，来创建index标记的列数,并赋值数据
     */
    private void createRowAndCell(Object obj, XSSFRow row, XSSFCell cell, int index) {
        cell = row.getCell(index);
        if (cell == null) {
            cell = row.createCell(index);
        }

        if (obj != null) {
            if (obj instanceof Integer) {
                cell.setCellValue(((Integer) obj).intValue());
            } else if (obj instanceof Double) {
                cell.setCellValue(((Double) obj).doubleValue());
            } else {
                cell.setCellValue(obj.toString());
            }
        } else
            cell.setCellValue("");
    }

    /**
     * 根据产业获取产品组
     *
     * @param factoryInventEntity
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/getNawCpz", method = RequestMethod.GET)
    public RetResult<Map<String, Object>> getNawCpz(HttpServletRequest request, HttpServletResponse response,
                                                    FactoryInventEntity factoryInventEntity) {
        Map<String, Object> param = new HashMap<>();
        List<String> plNames = null;
        if (!"".equals(factoryInventEntity.getNewplc1ode()) && factoryInventEntity.getNewplc1ode() != null) {
            plNames = Arrays.asList(factoryInventEntity.getNewplc1ode().split(","));
        }
        try {
            param.put("newplc1ode", plNames);
            List<FactoryInventEntity> list = factoryInventService.getCpzByCy(param);
            param.clear();
            param.put("subPlName", list);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return RetResponse.makeErrRsp(e.getMessage());
        }
        return RetResponse.makeOKRsp(param);
    }

    /**
     * 根据产品组获取工厂
     *
     * @param request
     * @param response
     * @param factoryInventEntity
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "getNewGc", method = RequestMethod.GET)
    public RetResult<Map<String, Object>> getNewGc(HttpServletRequest request, HttpServletResponse response,
                                                   FactoryInventEntity factoryInventEntity) {
        Map<String, Object> param = new HashMap<String, Object>();
        List<String> subPlNames = null;
        if (!"".equals(factoryInventEntity.getSubplname()) && factoryInventEntity.getSubplname() != null) {
            subPlNames = Arrays.asList(factoryInventEntity.getSubplname().split(","));
        }
        try {
            param.put("subplname", subPlNames);
            List<FactoryInventEntity> list = factoryInventService.getGcByCpz(param);
            param.clear();
            param.put("factoryList", list);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return RetResponse.makeErrRsp(e.getMessage());
        }
        return RetResponse.makeOKRsp(param);
    }

}

