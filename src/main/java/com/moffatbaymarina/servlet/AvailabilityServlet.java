/**Alexander Baldree
Max Jankowski
Aftabur Rahman
Jordan Dardar

Green team Module 5
Modified by Max on 9-4-26

*/

package com.moffatbaymarina.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moffatbaymarina.dao.SlipDAO;
import com.moffatbaymarina.dao.SlipTypeDAO;
import com.moffatbaymarina.model.SlipType;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


 // backing both the general how many slips of each size are open display and optionally a boat length look up that 
 // informs the customer which category slip their boat fits in , as well as the price.
 // login is not required for this. 
@WebServlet("/availability")
public class AvailabilityServlet extends HttpServlet {
    private static final ObjectMapper JSON = new ObjectMapper();

   	 
	 // Always to return every slip tyoe with its current open count. if the boatlength parameter is present 
	 //this will also respond with sliptype section. the smallest slip category that the boat fits in. How many of that size are there now, 
	 // plus the price 
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepareJson(response);
        BigDecimal boatLength = parseDecimal(request.getParameter("boatLength"));
        SlipTypeDAO slipTypeDAO = new SlipTypeDAO();
        SlipDAO slipDAO = new SlipDAO();

        try { // basic list, every slip type plus how many are open right now
            List<Map<String, Object>> availability = new ArrayList<>();
            for (SlipType slipType : slipTypeDAO.findAll()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("slipTypeId", slipType.getSlipTypeId());
                row.put("sizeFt", slipType.getSizeFt());
                row.put("totalCapacity", slipType.getTotalCapacity());
                row.put("ratePerFoot", slipType.getRatePerFoot());
                row.put("electricFee", slipType.getElectricFee());
                row.put("availableCount", slipDAO.countAvailable(slipType.getSlipTypeId()));
                availability.add(row);
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("slipTypes", availability);

            // boatLength is an optional field for this. only adding the extra "what does MY
            // boat need" section when the customer actually supplied one.
			// only bother with this part if they actually gave us a boat length
            if (boatLength != null && boatLength.compareTo(BigDecimal.ZERO) > 0) {
                SlipType required = slipTypeDAO.findRequiredForBoatLength(boatLength);
                if (required == null) {                    
                    body.put("requiredSlipType", null);// Boat is longer than the largest slip category offered.
                    body.put("message", "No slip type can accommodate this boat.");
                } else {  // same cost formula as everywhere else: length * rate + electric                    
                    
                    //Modification made here as the elec is now optional, thus estimate below is base cost only
                    // the electicalFee selected is seperate so that it can be seen what this option does to the total cost 9-10 Max
                    BigDecimal baseCost = boatLength.multiply(required.getRatePerFoot())
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal costWithElectric = baseCost
                            .add(required.getElectricFee())
                            .setScale(2, RoundingMode.HALF_UP);
                    Map<String, Object> requiredMap = new LinkedHashMap<>();
                    requiredMap.put("slipTypeId", required.getSlipTypeId());
                    requiredMap.put("sizeFt", required.getSizeFt());
                    requiredMap.put("availableCount",
                            slipDAO.countAvailable(required.getSlipTypeId()));
                    requiredMap.put("estimatedMonthlyCost", baseCost);
                    requiredMap.put("electricFeeIfSelected", required.getElectricFee());
                    requiredMap.put("estimatedMonthlyCostWithElectric", costWithElectric);
                    body.put("requiredSlipType", requiredMap);
                }
            }

            writeJson(response, HttpServletResponse.SC_OK, body);
        } catch (SQLException e) {
            log("Availability lookup error", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    error("Slip availability could not be loaded."));
        }
    }

	// will return null for missing boatLength. for this reason a null input is treated as no boat length instead of failed req. 
    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("message", message);
        return body;
    }

    private void prepareJson(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private void writeJson(HttpServletResponse response, int status, Object body)
            throws IOException {
        response.setStatus(status);
        JSON.writeValue(response.getWriter(), body);
    }
}
