#include <furi.h>
#include <furi_hal.h>
#include <gui/gui.h>
#include <gui/elements.h>
#include <input/input.h>
#include <notification/notification_messages.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>

// Constants
#define DEFAULT_START_FREQUENCY 100.0f
#define DEFAULT_BANDWIDTH 20.0f
#define DEFAULT_ALARM_THRESHOLD -50.0f
#define TONE_DURATION_MS 200
#define MIN_DELAY_MS 100
#define MAX_DELAY_MS 1000
#define MAX_SIGNAL_HISTORY 50

// Application structure
typedef struct {
    Gui* gui;
    NotificationApp* notifications;
    ViewPort* view_port;
    FuriMutex* mutex;
    
    // Frequency monitoring parameters
    float start_frequency;
    float bandwidth;
    float alarm_threshold;
    float current_signal_strength;
    
    // State
    bool is_monitoring;
    bool is_alarm_enabled;
    bool alarm_active;
    uint32_t last_alarm_time;
    
    // UI
    char status_message[128];
    char frequency_info[128];
    uint8_t signal_history[MAX_SIGNAL_HISTORY];
    uint8_t history_index;
} FrequencyMonitorApp;

// Forward declarations
static void frequency_monitor_input_callback(InputEvent* input_event, void* ctx);
static void frequency_monitor_render_callback(Canvas* canvas, void* ctx);
static void frequency_monitor_draw_ui(Canvas* canvas, FrequencyMonitorApp* app);
static void frequency_monitor_simulate_signal(FrequencyMonitorApp* app);

// Initialize application
static FrequencyMonitorApp* frequency_monitor_app_alloc() {
    FrequencyMonitorApp* app = malloc(sizeof(FrequencyMonitorApp));
    
    app->start_frequency = DEFAULT_START_FREQUENCY;
    app->bandwidth = DEFAULT_BANDWIDTH;
    app->alarm_threshold = DEFAULT_ALARM_THRESHOLD;
    app->current_signal_strength = -70.0f;
    
    app->is_monitoring = false;
    app->is_alarm_enabled = true;
    app->alarm_active = false;
    app->last_alarm_time = 0;
    
    app->history_index = 0;
    memset(app->signal_history, 0, sizeof(app->signal_history));
    
    snprintf(app->status_message, sizeof(app->status_message), "Ready");
    snprintf(app->frequency_info, sizeof(app->frequency_info), 
             "Range: %.1f - %.1f MHz", 
             app->start_frequency, 
             app->start_frequency + app->bandwidth);
    
    app->mutex = furi_mutex_alloc(FuriMutexTypeNormal);
    
    return app;
}

// Free application resources
static void frequency_monitor_app_free(FrequencyMonitorApp* app) {
    furi_mutex_free(app->mutex);
    free(app);
}

// Start monitoring
static void frequency_monitor_start(FrequencyMonitorApp* app) {
    furi_mutex_acquire(app->mutex, FuriWaitForever);
    app->is_monitoring = true;
    snprintf(app->status_message, sizeof(app->status_message), "Monitoring started...");
    furi_mutex_release(app->mutex);
    
    // Trigger notification
    notification_message(app->notifications, &sequence_blink_green_10);
}

// Stop monitoring
static void frequency_monitor_stop(FrequencyMonitorApp* app) {
    furi_mutex_acquire(app->mutex, FuriWaitForever);
    app->is_monitoring = false;
    app->alarm_active = false;
    snprintf(app->status_message, sizeof(app->status_message), "Monitoring stopped");
    furi_mutex_release(app->mutex);
    
    notification_message(app->notifications, &sequence_reset_red);
}

// Toggle alarm
static void frequency_monitor_toggle_alarm(FrequencyMonitorApp* app) {
    furi_mutex_acquire(app->mutex, FuriWaitForever);
    app->is_alarm_enabled = !app->is_alarm_enabled;
    furi_mutex_release(app->mutex);
}

// Calculate delay based on signal strength
static int frequency_monitor_calculate_delay(float signal_strength) {
    int calculated_delay = (int)((-signal_strength + 80) * 10);
    return (calculated_delay < MIN_DELAY_MS) ? MIN_DELAY_MS : 
           (calculated_delay > MAX_DELAY_MS) ? MAX_DELAY_MS : calculated_delay;
}

// Simulate signal detection
static void frequency_monitor_simulate_signal(FrequencyMonitorApp* app) {
    furi_mutex_acquire(app->mutex, FuriWaitForever);
    
    if (!app->is_monitoring) {
        furi_mutex_release(app->mutex);
        return;
    }
    
    // Simulate random signal fluctuation
    float random_offset = ((rand() % 20) - 10) / 10.0f;
    app->current_signal_strength = -60.0f + random_offset;
    
    // Store in history
    uint8_t strength_normalized = (uint8_t)((app->current_signal_strength + 100) * 2.55);
    app->signal_history[app->history_index] = strength_normalized;
    app->history_index = (app->history_index + 1) % MAX_SIGNAL_HISTORY;
    
    // Check alarm condition
    if (app->current_signal_strength > app->alarm_threshold) {
        app->alarm_active = true;
        snprintf(app->status_message, sizeof(app->status_message), 
                 "ALARM: Signal at %.1f dBm", app->current_signal_strength);
        
        // Trigger alarm notification
        if (app->is_alarm_enabled && furi_get_tick() - app->last_alarm_time > 
            frequency_monitor_calculate_delay(app->current_signal_strength)) {
            
            notification_message(app->notifications, &sequence_blink_red_100);
            app->last_alarm_time = furi_get_tick();
        }
    } else {
        app->alarm_active = false;
        snprintf(app->status_message, sizeof(app->status_message), 
                 "No alarm: Signal %.1f dBm", app->current_signal_strength);
    }
    
    furi_mutex_release(app->mutex);
}

// Input callback
static void frequency_monitor_input_callback(InputEvent* input_event, void* ctx) {
    FrequencyMonitorApp* app = ctx;
    
    if (input_event->type == InputTypePress) {
        switch (input_event->key) {
            case InputKeyUp:
                app->alarm_threshold += 5.0f;
                break;
            case InputKeyDown:
                app->alarm_threshold -= 5.0f;
                break;
            case InputKeyLeft:
                frequency_monitor_stop(app);
                break;
            case InputKeyRight:
                frequency_monitor_start(app);
                break;
            case InputKeyOk:
                frequency_monitor_toggle_alarm(app);
                break;
            case InputKeyBack:
                // Exit application
                furi_halt();
                break;
            default:
                break;
        }
    }
}

// Render callback
static void frequency_monitor_render_callback(Canvas* canvas, void* ctx) {
    FrequencyMonitorApp* app = ctx;
    
    furi_mutex_acquire(app->mutex, FuriWaitForever);
    frequency_monitor_draw_ui(canvas, app);
    furi_mutex_release(app->mutex);
}

// Draw UI
static void frequency_monitor_draw_ui(Canvas* canvas, FrequencyMonitorApp* app) {
    canvas_clear(canvas);
    
    // Header
    canvas_set_font(canvas, FontPrimary);
    canvas_draw_str(canvas, 5, 10, "Frequency Monitor");
    
    // Draw horizontal line
    canvas_draw_line(canvas, 0, 15, 128, 15);
    
    // Frequency info
    canvas_set_font(canvas, FontSecondary);
    canvas_draw_str(canvas, 5, 28, app->frequency_info);
    
    // Threshold display
    char threshold_str[32];
    snprintf(threshold_str, sizeof(threshold_str), "Threshold: %.1f dBm", app->alarm_threshold);
    canvas_draw_str(canvas, 5, 40, threshold_str);
    
    // Current signal strength
    char signal_str[32];
    snprintf(signal_str, sizeof(signal_str), "Signal: %.1f dBm", app->current_signal_strength);
    canvas_draw_str(canvas, 5, 52, signal_str);
    
    // Status message
    canvas_set_font(canvas, FontPrimary);
    canvas_draw_str(canvas, 5, 65, app->status_message);
    
    // Draw signal history graph
    canvas_set_color(canvas, ColorWhite);
    canvas_draw_frame(canvas, 5, 70, 118, 20);
    
    for (int i = 0; i < MAX_SIGNAL_HISTORY; i++) {
        uint8_t height = (app->signal_history[i] / 255) * 18;
        canvas_draw_line(canvas, 7 + i * 2, 87, 7 + i * 2, 87 - height);
    }
    
    // Status indicators
    canvas_set_font(canvas, FontSecondary);
    char status_indicator[32];
    snprintf(status_indicator, sizeof(status_indicator), 
             "%s | Alarm: %s",
             app->is_monitoring ? "ON" : "OFF",
             app->is_alarm_enabled ? "ON" : "OFF");
    canvas_draw_str(canvas, 5, 125, status_indicator);
    
    // Alarm active indicator
    if (app->alarm_active) {
        canvas_draw_box(canvas, 100, 5, 23, 8);
        canvas_set_color(canvas, ColorBlack);
        canvas_draw_str(canvas, 102, 11, "ALARM!");
        canvas_set_color(canvas, ColorWhite);
    }
    
    // Instructions
    canvas_set_font(canvas, FontSecondary);
    canvas_draw_str(canvas, 5, 125, "UP/DOWN: Threshold | LEFT/RIGHT: Control");
}

// Main application entry point
int32_t frequency_monitor_app(void* p) {
    UNUSED(p);
    
    FrequencyMonitorApp* app = frequency_monitor_app_alloc();
    
    // Acquire GUI
    app->gui = furi_record_open(RECORD_GUI);
    app->notifications = furi_record_open(RECORD_NOTIFICATION);
    
    // Create view port
    app->view_port = view_port_alloc();
    view_port_draw_callback_set(app->view_port, frequency_monitor_render_callback, app);
    view_port_input_callback_set(app->view_port, frequency_monitor_input_callback, app);
    
    // Add view port to GUI
    gui_add_view_port(app->gui, app->view_port, GuiLayerFullscreen);
    
    // Main loop
    FuriTimer* timer = furi_timer_alloc(frequency_monitor_simulate_signal, FuriTimerTypePeriodic, app);
    furi_timer_start(timer, 500); // Update every 500ms
    
    // Wait for exit
    furi_thread_get_current_priority();
    
    // Cleanup
    furi_timer_free(timer);
    gui_remove_view_port(app->gui, app->view_port);
    view_port_free(app->view_port);
    furi_record_close(RECORD_GUI);
    furi_record_close(RECORD_NOTIFICATION);
    
    frequency_monitor_app_free(app);
    
    return 0;
}
