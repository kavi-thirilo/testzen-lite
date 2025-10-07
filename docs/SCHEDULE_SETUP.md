# GitLab CI/CD Pipeline Schedule Setup

## Overview

This guide shows how to configure the TestZen pipeline to run automatically every night at 9 PM CST.

## Jobs Configuration

The pipeline now includes automated test jobs that run:
- On every push to any branch
- On merge requests
- On scheduled pipelines (nightly)
- Manual trigger (fallback option)

Jobs:
- `test:android:automated` - Android tests
- `test:ios:automated` - iOS tests

## Setting Up Nightly Schedule (9 PM CST)

### Step 1: Navigate to Pipeline Schedules

1. Go to your GitLab project
2. Click **CI/CD** in the left sidebar
3. Click **Schedules**
4. Click **New schedule** button

### Step 2: Configure the Schedule

Fill in the schedule form:

**Description:**
```
Nightly TestZen Tests - 9 PM CST
```

**Interval Pattern:**
```
Custom ( Learn more )
```

**Cron Syntax:**
```
0 2 * * *
```

**Explanation:**
- `0` = minute (0th minute)
- `2` = hour (2 AM UTC = 9 PM CST previous day)
- `*` = day of month (every day)
- `*` = month (every month)
- `*` = day of week (every day of week)

**Note:** CST is UTC-6, so 9 PM CST = 3 AM UTC (next day)
But during CDT (daylight saving), it's UTC-5, so 9 PM CDT = 2 AM UTC

**Cron timezone:** UTC

**Target branch:**
```
main
```
(or your default branch - could be `dev` or `testzen_ci_cd_integration`)

**Variables (optional):**
- Can add custom variables if needed
- Leave empty for now

**Active:**
- Check this box to activate the schedule

### Step 3: Click "Create pipeline schedule"

## Alternative: Year-Round 9 PM CST/CDT Schedule

To handle both CST (winter) and CDT (summer), you can:

### Option 1: Use 9 PM CST (3 AM UTC) year-round
```
0 3 * * *
```
During CDT (summer), this runs at 10 PM CDT

### Option 2: Use 9 PM CDT (2 AM UTC) year-round
```
0 2 * * *
```
During CST (winter), this runs at 8 PM CST

### Option 3: Create Two Schedules

**Winter Schedule (CST - Nov to Mar):**
```
Cron: 0 3 * * *
Active: Enabled (Enable Nov-Mar)
```

**Summer Schedule (CDT - Mar to Nov):**
```
Cron: 0 2 * * *
Active: Enabled (Enable Mar-Nov)
```

## Cron Syntax Reference

Format: `minute hour day_of_month month day_of_week`

**Examples:**

| Time | Cron Syntax | Description |
|------|-------------|-------------|
| 9 PM CST daily | `0 3 * * *` | Every day at 3 AM UTC |
| 9 PM CDT daily | `0 2 * * *` | Every day at 2 AM UTC |
| Midnight UTC | `0 0 * * *` | Every day at midnight |
| Every 6 hours | `0 */6 * * *` | 00:00, 06:00, 12:00, 18:00 UTC |
| Weekdays only 9PM CST | `0 3 * * 1-5` | Mon-Fri at 9PM CST |
| Every Monday 9PM CST | `0 3 * * 1` | Mondays only |

## Viewing Scheduled Pipelines

After creating the schedule:

1. Go to **CI/CD → Schedules**
2. You'll see your schedule listed
3. Click **Play** to test it immediately
4. View **Next Run** to confirm timing

## Schedule Actions

From **CI/CD → Schedules**, you can:

- **Play** - Run the pipeline immediately (for testing)
- **Edit** - Modify schedule settings
- **Take ownership** - Become the owner
- **Delete** - Remove the schedule

## Monitoring Scheduled Pipelines

### View Pipeline History

1. Go to **CI/CD → Pipelines**
2. Filter by **Trigger: Schedule**
3. See all scheduled pipeline runs

### Check Pipeline Results

Each scheduled run will show:
- Pipeline ID
- Triggered time
- Job results
- Test reports

## Environment Variables for Schedules

You can add variables specific to scheduled runs:

**In Schedule Settings:**

| Variable | Value | Description |
|----------|-------|-------------|
| `SCHEDULE_TYPE` | `nightly` | Identify as nightly run |
| `SEND_NOTIFICATIONS` | `true` | Enable email/Slack notifications |
| `TEST_ENVIRONMENT` | `production` | Use production config |

**Using in Jobs:**

```yaml
test:android:automated:
  script:
    - |
      if [ "$CI_PIPELINE_SOURCE" == "schedule" ]; then
        echo "Running scheduled tests with notifications"
        ./testzen run --all --notify
      else
        echo "Running regular tests"
        ./testzen run --all
      fi
```

## Notifications for Scheduled Runs

### Email Notifications

1. Go to **Settings → Integrations**
2. Configure **Pipeline emails**
3. Add recipient email addresses
4. Select **Pipeline failed** and **Pipeline succeeded**

### Slack Notifications

1. Go to **Settings → Integrations**
2. Configure **Slack notifications**
3. Add Slack webhook URL
4. Select events: **Pipeline** and **Failed pipeline**

### Custom Notifications in Pipeline

Add to your `.gitlab-ci.yml`:

```yaml
notify:results:
  stage: .post  # Runs after all other stages
  rules:
    - if: '$CI_PIPELINE_SOURCE == "schedule"'
  script:
    - |
      if [ "$CI_JOB_STATUS" == "success" ]; then
        curl -X POST $SLACK_WEBHOOK \
          -d '{"text":"Nightly TestZen tests passed!"}'
      else
        curl -X POST $SLACK_WEBHOOK \
          -d '{"text":"Nightly TestZen tests failed!"}'
      fi
```

## Testing the Schedule

### Method 1: Play Button

1. Go to **CI/CD → Schedules**
2. Click **Play** next to your schedule
3. Pipeline runs immediately
4. Check if jobs execute correctly

### Method 2: Temporary Schedule

Create a test schedule that runs in 5 minutes:

1. Get current UTC time: `date -u`
2. Add 5 minutes
3. Create schedule with that time
4. Wait and verify
5. Delete test schedule

## Timezone Reference

| Location | Timezone | UTC Offset | 9 PM Local = UTC |
|----------|----------|------------|------------------|
| CST (Winter) | America/Chicago | UTC-6 | 3 AM UTC next day |
| CDT (Summer) | America/Chicago | UTC-5 | 2 AM UTC next day |
| EST (Winter) | America/New_York | UTC-5 | 2 AM UTC next day |
| PST (Winter) | America/Los_Angeles | UTC-8 | 5 AM UTC next day |

**Calculate UTC from CST:**
- CST Time + 6 hours = UTC time (next day if > 18:00)
- 9 PM CST + 6 hours = 3 AM UTC

## Troubleshooting

### Schedule Not Running

**Check:**
1. Schedule is **Active** (checkbox enabled)
2. Target branch exists
3. Pipeline runs successfully on manual trigger
4. Cron syntax is valid
5. GitLab runners are available

### Wrong Time

**Fix:**
1. Verify your timezone offset
2. Recalculate UTC time
3. Update cron syntax
4. Test with "Play" button

### Jobs Skipped

**Check:**
1. Job rules include `$CI_PIPELINE_SOURCE == "schedule"`
2. Runners with required tags are online
3. Check job logs for why it was skipped

## Complete Example

Here's a complete schedule setup for 9 PM CST:

```
Description: Nightly TestZen Automated Tests
Interval Pattern: Custom
Cron: 0 3 * * *
Cron timezone: UTC
Target branch: main
Variables:
  SCHEDULE_TYPE = nightly
  RUN_FULL_SUITE = true
Active: Enabled
```

This will:
- Run every night at 9 PM CST (3 AM UTC)
- Execute all automated test jobs
- Use the `main` branch
- Set custom variables for identification

---

**Next Steps:**
1. Commit the updated `.gitlab-ci.yml`
2. Set up the schedule in GitLab UI
3. Test with "Play" button
4. Monitor first scheduled run
