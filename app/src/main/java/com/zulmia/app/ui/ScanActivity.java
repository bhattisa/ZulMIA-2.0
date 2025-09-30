package com.zulmia.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zulmia.app.R;
import com.zulmia.app.data.InventoryRepository;
import com.zulmia.app.util.CsvExporter;
import com.zulmia.app.util.SessionManager;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ScanActivity extends BaseActivity {
	private SessionManager session;
	private InventoryRepository repo;
    private ScanAdapter scanAdapter;
    private String currentCode = null;
    private String currentLoc = "";
    private String currentShelf = "";
    private EditText locationField;
    private EditText shelfField;
    private EditText scanInputField;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan);

        session = new SessionManager(this);
		repo = new InventoryRepository(this);

        attachToolbar(R.id.toolbar);
        androidx.appcompat.widget.Toolbar tb = findViewById(R.id.toolbar);
        setToolbarTitle(tb, "ZulMIA", "Inventory Count Application");

        // Keep focus but hide soft keyboard until user taps (scanner will input via hardware)
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		locationField = findViewById(R.id.location);
		shelfField = findViewById(R.id.shelf);
		scanInputField = findViewById(R.id.scanInput);
        TextView invInfo = findViewById(R.id.invInfo);
        Button submitOnly = findViewById(R.id.submitOnlyBtn);
        Button submitFinish = findViewById(R.id.submitFinishBtn);
        Button newInventory = findViewById(R.id.newInventoryBtn);
        Button homeBtn = findViewById(R.id.homeBtn);
        RecyclerView recycler = findViewById(R.id.recyclerScans);

        // Load inventory/session details early so inner classes can use them
        final long inventoryId = session.getInventoryId();
        final String inventoryName = session.getInventoryName();
        final String username = session.getUsername();
        if (inventoryId == -1 || username == null) {
            finish();
            return;
        }
        scanAdapter = new ScanAdapter(new ScanAdapter.OnRowActionListener() {
            @Override
            public void onIncrease(InventoryRepository.AggregatedScanRow row) {
                int newQty = row.qty + 1;
                repo.updateScanQuantity(inventoryId, row.location == null ? "" : row.location, row.shelf == null ? "" : row.shelf, row.code, newQty, System.currentTimeMillis());
                refreshList(scanAdapter, inventoryId);
            }

            @Override
            public void onDecrease(InventoryRepository.AggregatedScanRow row) {
                int newQty = row.qty - 1;
                repo.updateScanQuantity(inventoryId, row.location == null ? "" : row.location, row.shelf == null ? "" : row.shelf, row.code, newQty, System.currentTimeMillis());
                refreshList(scanAdapter, inventoryId);
            }

            @Override
            public void onEditRow(InventoryRepository.AggregatedScanRow row) {
                showEditDialog(row, inventoryId);
            }
        });
		recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(scanAdapter);
        ItemTouchHelper ith = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
			public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
				int pos = viewHolder.getBindingAdapterPosition();
				InventoryRepository.AggregatedScanRow row = scanAdapter.getItem(pos);
				if (row == null) {
					scanAdapter.notifyItemChanged(pos);
					return;
				}
				new androidx.appcompat.app.AlertDialog.Builder(ScanActivity.this)
						.setTitle("Remove item?")
						.setMessage("This will remove the scanned item from this inventory.")
						.setPositiveButton("Remove", (d, w) -> {
							repo.deleteScan(inventoryId,
									row.location == null ? "" : row.location,
									row.shelf == null ? "" : row.shelf,
									row.code);
							refreshList(scanAdapter, inventoryId);
						})
						.setNegativeButton("Cancel", (d, w) -> {
							scanAdapter.notifyItemChanged(pos);
						})
						.setOnCancelListener(dialog -> scanAdapter.notifyItemChanged(pos))
						.show();
			}
        });
        ith.attachToRecyclerView(recycler);

        // inventoryId, inventoryName, username already initialized above
        // Bold only the inventory and user names, not the labels
        String header = "Inventory: " + inventoryName + "\nUser: " + username;
        String defLoc = session.getDefaultLocation();
        if (defLoc != null && !defLoc.isEmpty()) {
            locationField.setText(defLoc);
            locationField.setEnabled(false);
            shelfField.requestFocus();
        }
        android.text.SpannableString styled = new android.text.SpannableString(header);
        int invStart = "Inventory: ".length();
        int invEnd = invStart + inventoryName.length();
        styled.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), invStart, invEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        int userLabelLen = ("Inventory: " + inventoryName + "\nUser: ").length();
        int userEnd = userLabelLen + username.length();
        styled.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), userLabelLen, userEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        invInfo.setText(styled);

		locationField.requestFocus();
        refreshList(scanAdapter, inventoryId);
		locationField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
					String val = locationField.getText().toString().trim();
					locationField.setText(val);
					shelfField.requestFocus();
					return true;
				}
				return false;
			}
		});
		shelfField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
					String val = shelfField.getText().toString().trim();
					shelfField.setText(val);
					scanInputField.requestFocus();
					return true;
				}
				return false;
			}
		});
			scanInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_NULL) {
					String code = scanInputField.getText().toString();
					String locVal = locationField.getText().toString();
					String shelfVal = shelfField.getText().toString();
                    handleScan(code, locVal, shelfVal);
                    // Track current selection for quantity edits
                    currentCode = code;
                    currentLoc = locVal == null ? "" : locVal;
                    currentShelf = shelfVal == null ? "" : shelfVal;
                    // After scanning, reflect aggregated qty for this code in qtyInput (if found)
                    // no qty input; dialog handles manual edits
					scanInputField.setText("");
					refreshList(scanAdapter, inventoryId);
					boolean clearShelf = SettingsActivity.isClearShelfOnScanEnabled(ScanActivity.this);
					if (clearShelf) {
						shelfField.setText("");
						scanInputField.clearFocus();
						shelfField.requestFocus();
						shelfField.post(new Runnable() {
							@Override
							public void run() {
								shelfField.requestFocus();
								try {
									android.text.Editable t = shelfField.getText();
									if (t != null) shelfField.setSelection(t.length());
								} catch (Exception ignored) { }
							}
						});
					} else {
						scanInputField.requestFocus();
					}
					return true;
				}
				return false;
			}
		});

			scanInputField.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }

			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() > 0 && s.charAt(s.length() - 1) == '\n') {
					String code = s.toString().trim();
					String locVal = locationField.getText().toString();
					String shelfVal = shelfField.getText().toString();
                    handleScan(code, locVal, shelfVal);
                    currentCode = code;
                    currentLoc = locVal == null ? "" : locVal;
                    currentShelf = shelfVal == null ? "" : shelfVal;
                    // no qty input; dialog handles manual edits
					scanInputField.setText("");
					refreshList(scanAdapter, inventoryId);
					boolean clearShelf = SettingsActivity.isClearShelfOnScanEnabled(ScanActivity.this);
					if (clearShelf) {
						shelfField.setText("");
						scanInputField.clearFocus();
						shelfField.requestFocus();
						shelfField.post(new Runnable() {
							@Override
							public void run() {
								shelfField.requestFocus();
								try {
									android.text.Editable t = shelfField.getText();
									if (t != null) shelfField.setSelection(t.length());
								} catch (Exception ignored) { }
							}
						});
					} else {
						scanInputField.requestFocus();
					}
				}
			}
		});

        View.OnClickListener exportHandler = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                List<InventoryRepository.AggregatedScanRow> rows = repo.getAggregatedScans(inventoryId);
				try {
                    String existing = repo.getInventoryCsvName(inventoryId);
                    String exportFormat = SettingsActivity.getExportFormat(ScanActivity.this);
                    CsvExporter.ExportResult result = CsvExporter.exportInventory(ScanActivity.this, username, inventoryName, inventoryId, System.currentTimeMillis(), rows, existing, exportFormat);
					String name = result.displayName != null ? result.displayName : "inventory.csv";
                    if (existing == null || existing.isEmpty()) {
                        repo.setInventoryCsvName(inventoryId, name);
                    }
					Snackbar.make(findViewById(android.R.id.content), "Exported: " + name, Snackbar.LENGTH_LONG)
							.setAction("Open", new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									try {
										Uri uri;
										if (result.contentUri != null) {
											uri = result.contentUri;
										} else if (result.file != null) {
											uri = FileProvider.getUriForFile(ScanActivity.this, "com.zulmia.app.fileprovider", result.file);
										} else {
											Toast.makeText(ScanActivity.this, "File not available", Toast.LENGTH_SHORT).show();
											return;
										}
										openCsvWithFallback(uri);
									} catch (Exception e) {
										Toast.makeText(ScanActivity.this, "Unable to open file", Toast.LENGTH_SHORT).show();
									}
								}
							}).show();
				} catch (IOException e) {
					Toast.makeText(ScanActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
				}
			}
        };

        submitOnly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do not export on simple submit; keep inventory open
                Snackbar.make(findViewById(android.R.id.content), "Saved. You can resume from Pending Inventories.", Snackbar.LENGTH_SHORT).show();
            }
        });

        submitFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<InventoryRepository.AggregatedScanRow> rows = repo.getAggregatedScans(inventoryId);
                String existing = repo.getInventoryCsvName(inventoryId);
                try {
                    String exportFormat = SettingsActivity.getExportFormat(ScanActivity.this);
                    CsvExporter.ExportResult result = CsvExporter.exportInventory(ScanActivity.this, username, inventoryName, inventoryId, System.currentTimeMillis(), rows, existing, exportFormat);
                    String name = result.displayName != null ? result.displayName : (existing != null ? existing : "inventory.csv");
                    if (existing == null || existing.isEmpty()) {
                        repo.setInventoryCsvName(inventoryId, name);
                    }
                    Snackbar.make(findViewById(android.R.id.content), "Exported: " + name, Snackbar.LENGTH_LONG)
                            .setAction("Open", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        Uri uri;
                                        if (result.contentUri != null) {
                                            uri = result.contentUri;
                                        } else if (result.file != null) {
                                            uri = FileProvider.getUriForFile(ScanActivity.this, "com.zulmia.app.fileprovider", result.file);
                                        } else {
                                            Toast.makeText(ScanActivity.this, "File not available", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                        openCsvWithFallback(uri);
                                    } catch (Exception e) {
                                        Toast.makeText(ScanActivity.this, "Unable to open file", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }).show();
                } catch (IOException e) {
                    Toast.makeText(ScanActivity.this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
                repo.setInventoryStatus(inventoryId, 1);
                session.clearInventory();
            }
        });

        newInventory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ScanActivity.this, InventoryActivity.class));
                finish();
            }
        });

        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ScanActivity.this, MainActivity.class));
                finish();
            }
        });
	}

    private void handleScan(String code, String location, String shelf) {
		if (code == null || code.isEmpty()) {
			return;
		}
		long inventoryId = session.getInventoryId();
		String username = session.getUsername();
		repo.upsertScan(inventoryId, location == null ? "" : location, shelf == null ? "" : shelf, code, username, System.currentTimeMillis());
		// No toast on each scan as per requirement
	}

    private int getCurrentQty(long inventoryId, String location, String shelf, String code) {
        List<InventoryRepository.AggregatedScanRow> rows = repo.getAggregatedScans(inventoryId);
        if (rows == null) return -1;
        String loc = location == null ? "" : location;
        String sh = shelf == null ? "" : shelf;
        for (InventoryRepository.AggregatedScanRow r : rows) {
            String rloc = r.location == null ? "" : r.location;
            String rsh = r.shelf == null ? "" : r.shelf;
            if (rloc.equals(loc) && rsh.equals(sh) && r.code.equals(code)) {
                return r.qty;
            }
        }
        return -1;
    }

    // qty input removed; manual edits occur via dialog on item tap

    private void showEditDialog(InventoryRepository.AggregatedScanRow row, long inventoryId) {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_qty, null);
        TextView label = view.findViewById(R.id.labelCode);
        EditText editCode = view.findViewById(R.id.editCode);
        EditText editQty = view.findViewById(R.id.editQty);
        label.setText("Barcode");
        editCode.setText(row.code);
        editCode.setSelection(editCode.getText().length());
        editQty.setText(String.valueOf(row.qty));
        editQty.setSelection(editQty.getText().length());
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Edit Item")
                .setView(view)
                .setPositiveButton("Save", (d, which) -> {
                    String newCode = editCode.getText().toString().trim();
                    String qtyTxt = editQty.getText().toString().trim();
                    try {
                        int newQty = Integer.parseInt(qtyTxt);
                        if (newQty < 0) newQty = 0;
                        String loc = row.location == null ? "" : row.location;
                        String sh = row.shelf == null ? "" : row.shelf;
                        // Update code if changed
                        if (!newCode.equals(row.code)) {
                            repo.updateScanCode(inventoryId, loc, sh, row.code, newCode);
                        }
                        // Update qty
                        repo.updateScanQuantity(inventoryId, loc, sh, newCode, newQty, System.currentTimeMillis());
                        refreshList(scanAdapter, inventoryId);
                    } catch (NumberFormatException ignored) { }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dlg.show();
    }

	private void refreshList(ScanAdapter adapter, long inventoryId) {
		List<InventoryRepository.AggregatedScanRow> rows = repo.getAggregatedScans(inventoryId);
		adapter.setItems(rows);
	}

	private void openCsvWithFallback(Uri uri) {
		try {
                                        Intent viewCsv = new Intent(Intent.ACTION_VIEW);
                                        viewCsv.setDataAndType(uri, "text/csv");
                                        viewCsv.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        if (canHandle(viewCsv)) {
                                            startActivity(Intent.createChooser(viewCsv, "Open CSV"));
                                            return;
                                        }
                                        Intent viewTxt = new Intent(Intent.ACTION_VIEW);
                                        viewTxt.setDataAndType(uri, "text/plain");
                                        viewTxt.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        if (canHandle(viewTxt)) {
                                            startActivity(Intent.createChooser(viewTxt, "Open File"));
                                            return;
                                        }
                                        Intent viewAny = new Intent(Intent.ACTION_VIEW);
                                        viewAny.setDataAndType(uri, "application/octet-stream");
                                        viewAny.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        if (canHandle(viewAny)) {
                                            startActivity(Intent.createChooser(viewAny, "Open File"));
                                            return;
                                        }
                                        Intent viewer = new Intent(ScanActivity.this, CsvViewerActivity.class);
                                        viewer.setData(uri);
                                        viewer.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        startActivity(viewer);
			return;
		} catch (Exception ignored) { }
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(uri, "text/plain");
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(Intent.createChooser(intent, "Open File"));
			return;
		} catch (Exception ignored) { }
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(uri, "application/octet-stream");
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			startActivity(Intent.createChooser(intent, "Open File"));
		} catch (Exception e) {
			Toast.makeText(this, "No app to open file", Toast.LENGTH_SHORT).show();
		}
	}

	private boolean canHandle(Intent intent) {
		PackageManager pm = getPackageManager();
		List<ResolveInfo> handlers = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return handlers != null && !handlers.isEmpty();
	}
}


