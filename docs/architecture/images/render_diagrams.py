"""Render reviewable PNG previews for the hand-editable SVG architecture diagrams."""

from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import textwrap

ROOT = Path(__file__).parent
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
PALETTE = {
    "blue_fill": "#dbeafe", "blue_stroke": "#1e40af", "app_fill": "#93c5fd",
    "app_stroke": "#1e3a5f", "orange_fill": "#fed7aa", "orange_stroke": "#c2410c",
    "green_fill": "#a7f3d0", "green_stroke": "#047857", "yellow_fill": "#fef3c7",
    "yellow_stroke": "#b45309", "red_fill": "#fee2e2", "red_stroke": "#dc2626",
    "title": "#1e40af", "body": "#374151", "muted": "#64748b", "line": "#1e3a5f",
}


def font(size, bold=False):
    path = FONT.replace("DejaVuSans.ttf", "DejaVuSans-Bold.ttf") if bold else FONT
    return ImageFont.truetype(path, size)


def wrap(draw, text, fnt, max_width):
    words, rows, row = text.split(), [], ""
    for word in words:
        candidate = f"{row} {word}".strip()
        if row and draw.textbbox((0, 0), candidate, font=fnt)[2] > max_width:
            rows.append(row)
            row = word
        else:
            row = candidate
    if row:
        rows.append(row)
    return rows


def centered(draw, text, xy, fnt, fill, max_width):
    x, y = xy
    rows = wrap(draw, text, fnt, max_width)
    line_h = fnt.size + 6
    for i, row in enumerate(rows):
        width = draw.textbbox((0, 0), row, font=fnt)[2]
        draw.text((x - width / 2, y + i * line_h), row, font=fnt, fill=fill)


def box(draw, xywh, title, lines, fill, stroke, title_size=20, body_size=16, radius=16):
    x, y, w, h = xywh
    draw.rounded_rectangle((x, y, x + w, y + h), radius=radius, fill=fill, outline=stroke, width=3)
    title_font, body_font = font(title_size, True), font(body_size)
    centered(draw, title, (x + w / 2, y + 17), title_font, PALETTE["body"], w - 24)
    yy = y + 17 + title_size + 12
    for line in lines:
        rows = wrap(draw, line, body_font, w - 24)
        for row in rows:
            tw = draw.textbbox((0, 0), row, font=body_font)[2]
            draw.text((x + (w - tw) / 2, yy), row, font=body_font, fill=PALETTE["body"])
            yy += body_size + 5


def arrow(draw, start, end, color=None, width=3):
    color = color or PALETTE["line"]
    draw.line((*start, *end), fill=color, width=width)
    import math
    dx, dy = end[0] - start[0], end[1] - start[1]
    length = max(1, math.hypot(dx, dy))
    ux, uy = dx / length, dy / length
    px, py = -uy, ux
    tip, back, half = end, (end[0] - ux * 15, end[1] - uy * 15), 7
    draw.polygon((tip, (back[0] + px * half, back[1] + py * half),
                  (back[0] - px * half, back[1] - py * half)), fill=color)


def base(title, subtitle, size=(1600, 980)):
    im = Image.new("RGB", size, "white")
    d = ImageDraw.Draw(im)
    d.text((60, 42), title, font=font(34, True), fill=PALETTE["title"])
    d.text((60, 91), subtitle, font=font(18), fill=PALETTE["muted"])
    return im, d


def architecture():
    im, d = base("Claims workflow architecture", "One Spring Boot service • PostgreSQL transaction is the consistency boundary")
    box(d, (55, 155, 250, 130), "Browser UI", ["Basic sign-in • CSRF", "Idempotency-Key"], PALETTE["orange_fill"], PALETTE["orange_stroke"])
    box(d, (375, 155, 330, 130), "REST + security", ["Actor from principal", "Effective correlation ID", "Bounded JSON"], PALETTE["app_fill"], PALETTE["app_stroke"])
    box(d, (785, 155, 340, 130), "Claim use cases", ["Role / state / guard / version", "Advisory key lock + row lock"], PALETTE["app_fill"], PALETTE["app_stroke"])
    arrow(d, (305, 220), (365, 220)); arrow(d, (715, 220), (775, 220)); arrow(d, (950, 285), (950, 348))

    d.rounded_rectangle((330, 350, 1290, 720), radius=24, fill="#f8fafc", outline=PALETTE["line"], width=3)
    d.text((365, 370), "ONE DATABASE TRANSACTION", font=font(21, True), fill=PALETTE["line"])
    d.text((365, 400), "Claim, audit, receipt and durable effect request commit or roll back together.", font=font(15), fill=PALETTE["muted"])
    box(d, (370, 450, 245, 105), "claims", ["row lock + version CAS"], PALETTE["green_fill"], PALETTE["green_stroke"])
    box(d, (660, 450, 275, 105), "Audit records", ["domain + system operation"], PALETTE["green_fill"], PALETTE["green_stroke"])
    box(d, (980, 450, 270, 105), "command_receipts", ["actor • scope • key • hash"], PALETTE["green_fill"], PALETTE["green_stroke"])
    box(d, (485, 590, 245, 95), "payment_requests", ["created on approval"], PALETTE["yellow_fill"], PALETTE["yellow_stroke"])
    box(d, (800, 590, 285, 95), "local_outbox", ["effect intent persisted"], PALETTE["yellow_fill"], PALETTE["yellow_stroke"])
    arrow(d, (615, 500), (650, 500)); arrow(d, (935, 500), (970, 500))
    arrow(d, (490, 555), (560, 580)); arrow(d, (730, 640), (790, 640))

    box(d, (1360, 425, 200, 190), "PostgreSQL 16", ["Liquibase", "Restricted runtime", "Append-only audit"], PALETTE["blue_fill"], PALETTE["blue_stroke"])
    arrow(d, (1295, 535), (1375, 535))
    box(d, (815, 785, 275, 105), "Scheduled dispatcher", ["lease • retry • batch"], PALETTE["yellow_fill"], PALETTE["yellow_stroke"])
    box(d, (1160, 785, 310, 105), "Mock effect delivery", ["payment reference → confirmed"], PALETTE["yellow_fill"], PALETTE["yellow_stroke"])
    arrow(d, (940, 685), (940, 775), PALETTE["green_stroke"]); arrow(d, (1095, 835), (1150, 835), PALETTE["green_stroke"])
    arrow(d, (1470, 785), (1470, 625), PALETTE["green_stroke"])

    box(d, (55, 520, 235, 140), "Rejected mutation", ["business write rolls back", "failure audit uses", "REQUIRES_NEW"], PALETTE["red_fill"], PALETTE["red_stroke"], body_size=15)
    d.line((820, 285, 820, 320, 315, 320, 315, 590, 290, 590), fill=PALETTE["red_stroke"], width=3)
    d.polygon(((290, 590), (305, 582), (305, 598)), fill=PALETTE["red_stroke"])
    d.text((55, 925), "Observability: X-Correlation-ID • structured request logs • Micrometer observations • Prometheus metrics", font=font(15), fill=PALETTE["muted"])
    im.save(ROOT / "claims-architecture.png")


def workflow():
    im, d = base("Configured claim workflow", "Eight states • nine role-guarded edges • exact edge definitions in workflow.yml", (1600, 1100))
    nodes = {
        "SUBMITTED": (60, 200, 205, 90, PALETTE["orange_fill"], PALETTE["orange_stroke"]),
        "DOCUMENTS_VERIFIED": (360, 200, 235, 90, PALETTE["blue_fill"], PALETTE["blue_stroke"]),
        "UNDER_ASSESSMENT": (700, 190, 245, 110, PALETTE["blue_fill"], PALETTE["blue_stroke"]),
        "APPROVED": (1175, 145, 205, 90, PALETTE["green_fill"], PALETTE["green_stroke"]),
        "REJECTED": (1175, 285, 205, 90, PALETTE["blue_fill"], PALETTE["blue_stroke"]),
        "PENDING_INFO": (700, 450, 245, 100, PALETTE["blue_fill"], PALETTE["blue_stroke"]),
        "PAYMENT_INITIATED": (1175, 455, 245, 95, PALETTE["blue_fill"], PALETTE["blue_stroke"]),
        "CLOSED": (1175, 700, 205, 95, PALETTE["green_fill"], PALETTE["green_stroke"]),
    }
    centers = {}
    for name, (x,y,w,h,fill,stroke) in nodes.items():
        d.rounded_rectangle((x,y,x+w,y+h), radius=22, fill=fill, outline=stroke, width=3)
        centered(d, name, (x+w/2,y+(h-22)/2), font(18,True), PALETTE["body"], w-20)
        centers[name]=(x,y,w,h)

    # Connector routes and labels are kept separate so each edge stays visible.
    arrows=[((265,245),(350,245)),((595,245),(690,245)),((945,205),(1165,190)),((945,275),(1165,330)),((820,305),(820,440)),((700,500),(605,290)),((1295,555),(1295,690))]
    for a,b in arrows: arrow(d,a,b)
    # Payment starts at APPROVED and routes around REJECTED to the payment state.
    d.line((1380, 190, 1470, 190, 1470, 500, 1430, 500), fill=PALETTE["line"], width=3)
    d.polygon(((1430,500),(1445,492),(1445,508)),fill=PALETTE["line"])
    # The rejected closure edge bends around the payment state instead of crossing it.
    d.line((1175, 330, 1080, 380, 1080, 620, 1175, 700), fill=PALETTE["line"], width=3)
    d.polygon(((1175,700),(1159,694),(1168,682)),fill=PALETTE["line"])
    # Number markers connect the flow to the concrete transition key below.
    markers=[(305,222,1),(645,222,2),(1055,178,3),(1055,310,4),(845,365,5),(645,400,6),(1470,350,7),(1320,620,8),(1070,500,9)]
    for x,y,n in markers:
        d.ellipse((x-15,y-15,x+15,y+15),fill="#ffffff",outline=PALETTE["line"],width=2)
        tw=d.textbbox((0,0),str(n),font=font(15,True))[2]
        d.text((x-tw/2,y-11),str(n),font=font(15,True),fill=PALETTE["title"])
    d.rounded_rectangle((60, 810, 1510, 1045), radius=18, fill="#f8fafc", outline="#94a3b8", width=2)
    d.text((95, 830), "Transition key — role and guard", font=font(21,True), fill=PALETTE["body"])
    items=[
      "1  verify-documents · document_clerk · all required documents valid",
      "2  start-assessment · team_lead · assessor assigned",
      "3  approve-claim · assessor · complete report and amount within limit",
      "4  reject-claim · assessor · complete report and reason present",
      "5  request-information · assessor · maximum three cycles",
      "6  return-with-information · document_clerk · new information received",
      "7  initiate-payment · finance · persisted payment request",
      "8  close-paid-claim · finance · payment confirmed with reference",
      "9  close-rejected-claim · system · appeal expired or member acknowledged",
    ]
    for i,item in enumerate(items):
        col=i%3; row=i//3; x=95+col*475; y=875+row*54
        for j,line in enumerate(wrap(d,item,font(14),445)):
            d.text((x,y+j*18),line,font=font(14),fill=PALETTE["muted"])
    d.text((60, 1070), "State and transition IDs are defined in src/main/resources/config/workflow.yml.", font=font(16), fill=PALETTE["muted"])
    im.save(ROOT / "claims-workflow.png")


if __name__ == "__main__":
    architecture()
    workflow()
