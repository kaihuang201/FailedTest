function toggleReg() {
	jQuery('#mytable tbody tr td:contains("Newly Failed")').parent().toggle();
}
function toggleProg(){
	jQuery('#mytable tbody tr td:contains("Newly Passed")').parent().toggle();
}

clearFilters = function(tableID) {
	jQuery('#'+tableID+'Filters').find('input[type="text"],input[type="number"],select').each(function() {
		jQuery(this).val('');
	});
	
	jQuery('#'+tableID).find('tbody').find('tr:hidden').each(function() {
		jQuery(this).show();
	});
}

filterBuilds = function() {
	var buildStatus = jQuery('#buildStatus').val();
	var author = jQuery('#author').val();
	
	var minPassingRate = jQuery('#minPassingRate').val();
	if (minPassingRate != '') {
		minPassingRate = parseFloat(minPassingRate);
	}

	var minPassedTests = jQuery('#minPassedTests').val();
	if (minPassedTests != '') {
		minPassedTests = parseInt(minPassedTests);
	}
	
	jQuery('#tblBuilds').find('tbody').find('tr').each(function() {
		if (buildStatus != '') {
			var status = jQuery(this).find('td[data-col="status"]').text();
			if (status != buildStatus) {
				jQuery(this).hide();
				return;
			}
		}

		if (author != '') {
			var authors = jQuery(this).find('td[data-col="authors"]').text().split(", ");
			if (jQuery.inArray(author, authors) == -1) {
				jQuery(this).hide();
				return;
			}
		}
		
		if (minPassingRate != '') {
			var passingRate = parseFloat(jQuery(this).find('td[data-col="passingRate"]').text());
			if (passingRate < minPassingRate) {
				jQuery(this).hide();
				return;
			}
		}

		if (minPassedTests != '') {
			var passedTests = parseInt(jQuery(this).find('td[data-col="passedTests"]').text());
			if (passedTests < minPassedTests) {
				jQuery(this).hide();
				return;
			}
		}
		
		jQuery(this).show();
	});
}

filterTests = function() {
	var className = jQuery('#className').val();
	var testName = jQuery('#testName').val();
	
	var minFailingRate = jQuery('#minFailingRate').val();
	if (minFailingRate != '') {
		minFailingRate = parseFloat(minFailingRate);
	}

	jQuery('#tblTestList').find('tbody').find('tr').each(function() {
		if (className != '') {
			var colClass = jQuery(this).find('td[data-col="className"]').text();
			if (colClass.toLowerCase().indexOf(className.toLowerCase()) == -1) {
				jQuery(this).hide();
				return;
			}
		}
		
		if (testName != '') {
			var colTestName = jQuery(this).find('td[data-col="name"]').text();
			if (colTestName.toLowerCase().indexOf(testName.toLowerCase()) == -1) {
				jQuery(this).hide();
				return;
			}
		}

			
		if (minFailingRate != '') {
			var failingRate = parseFloat(jQuery(this).find('td[data-col="failingRate"]').text());
			if (failingRate < minFailingRate) {
				jQuery(this).hide();
				return;
			}
		}

		jQuery(this).show();
	});
}

searchTests = function() {
	var searchText = jQuery('#txtSearch').val();
	remoteAction.searchTests(searchText, jQuery.proxy(function(t) {
		var results = t.responseObject();

		var tbody = jQuery('#tblSearchResults').find('tbody');
		tbody.empty();

		results.forEach(function(test) {
			var tdAction = jQuery('<td data-col="action"></td>');
			tdAction.append('<button type="button" onclick="addTestToChart(this);">Add</button>');
			tdAction.append('<input type="hidden" name="testFullName" value="' + test.fullName + '" />');

			var tr = jQuery('<tr></tr>');
			tr.append(tdAction);
			tr.append('<td>' + test.packageName + '</td>');
			tr.append('<td data-col="className">' + test.className + '</td>');
			tr.append('<td data-col="testName">' + test.name + '</td>');
			tr.append('<td data-col="passedCount">' + test.passedCount + '</td>');
			tr.append('<td data-col="failedCount">' + test.failedCount + '</td>');
			tr.append('<td data-col="skippedCount">' + test.skippedCount + '</td>');

			tbody.append(tr);
		});
		
		toggleAddTestToChartButtons();
	}, this));
}

addTestToChart = function(addButton) {
	var testRow = jQuery(addButton).closest('tr');
	var testFullName = testRow.find('input[name="testFullName"]').val();

	/* Ensure test has not been added to the chart data */
	var valid = true;
	jQuery('#tblChartData').find('input[name="testFullName"]').each(function() {
		if (jQuery(this).val() == testFullName) {
			valid = false;
			return false;
		}
	});

	/* Add test to chart data */
	if (valid) {
		/* Create a copy of the row */
		var tr = testRow.clone();
		var tdAction = tr.find('td[data-col="action"]');
		
		/* Remove add button */
		tdAction.find('button').remove();
		
		/* Add delete button */
		tdAction.prepend('<button type="button" onclick="removeTestFromChart(this);">Remove</button>');
		
		/* Add row to chart data */
		jQuery('#tblChartData').find('tbody').append(tr);

		toggleAddTestToChartButtons();
	}
}

removeTestFromChart = function(deleteButton) {
	jQuery(deleteButton).closest('tr').remove();

	toggleAddTestToChartButtons();
}

toggleAddTestToChartButtons = function() {
	var maxTests = 5;
	var currentCount = jQuery('#tblChartData').find('tbody').find('tr').length;

	if (currentCount >= maxTests) {
		jQuery('#tblSearchResults').find('td[data-col="action"]').find('button:enabled').each(function() {
			jQuery(this).prop('disabled', true);
		});
	}
	else {
		jQuery('#tblSearchResults').find('td[data-col="action"]').find('button:disabled').each(function() {
			jQuery(this).prop('disabled', false);
		});
	}
}

generateTestCharts = function() {
	var chartType = jQuery('input[name="rdbChartType"]:checked').val();
	jQuery('#divCharts').empty();
	
	if (chartType == 'bar') {
		generateBarChart(false);
	}
	else if (chartType == 'stackedbar') {
		generateBarChart(true);
	}
	else if (chartType == 'pie') {
		generatePieCharts();
	}
}

generateBarChart = function(stacked) {
	 var data = new google.visualization.DataTable;
	 
	 data.addColumn('string', 'Test Name');
	 data.addColumn('number', 'Passed');
	 data.addColumn('number', 'Failed');
	 data.addColumn('number', 'Skipped');
     var i = 0;
     jQuery('#tblChartData').find('tbody').find('tr').each(function() {
          data.addRow();
          data.setCell(i, 0, jQuery(this).find('td[data-col="className"]').text() + '.' + jQuery(this).find('td[data-col="testName"]').text());
          data.setCell(i, 1, parseInt(jQuery(this).find('td[data-col="passedCount"]').text()));
          data.setCell(i, 2, parseInt(jQuery(this).find('td[data-col="failedCount"]').text()));
          data.setCell(i, 3, parseInt(jQuery(this).find('td[data-col="skippedCount"]').text()));
          
          i++;
     });
     var options = {
             width: '80%',
             height: 400,
             legend: { position: 'top', maxLines: 3 },
             bar: { groupWidth: '75%' },
             isStacked: stacked
      };
     var chart = new google.visualization.ColumnChart(document.getElementById('divCharts'));
     chart.draw(data, options);  
}

generatePieCharts = function() {
    var data1 = google.visualization.arrayToDataTable([
       ['Data', 'Total Tests'],
       ['Pass',  0],
       ['Fail',  0],
       ['Skip',  0]
     ]);
	
    var i = 0;
    jQuery('#tblChartData').find('tbody').find('tr').each(function() {
    	data1.setCell(0, 1, jQuery(this).find('td[data-col="passedCount"]').text());
    	data1.setCell(1, 1, jQuery(this).find('td[data-col="failedCount"]').text());
    	data1.setCell(2, 1, jQuery(this).find('td[data-col="skippedCount"]').text());
        var options1 = {
    		title: jQuery(this).find('td[data-col="className"]').text() + '.' +jQuery(this).find('td[data-col="testName"]').text()
	    };
        
        var pieDiv = jQuery('#divCharts');
        pieDiv.append('<div class = "tb-chart" id = "c'+i+'"></div>');
        var chart1 = new google.visualization.PieChart(document.getElementById("c"+i));
        chart1.draw(data1, options1);
        i++;
    });



}